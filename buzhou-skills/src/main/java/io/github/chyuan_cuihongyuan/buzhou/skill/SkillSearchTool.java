package io.github.chyuan_cuihongyuan.buzhou.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.ToolCallback;

/**
 * 技能检索工具（spec 37 §A / T132 / impl-105）：目录受注入预算截断（spec 35 §B）后，
 * 模型经本工具按名称/描述子串检索**可见全集**（不受 catalog-max-entries 限制）——
 * 命中后可 {@code load_skill(name)} 加载正文。
 *
 * <p>检索语义：名称与 description 不分大小写子串匹配；返回上限 20 条
 * （name + description + 命中字段）；绑定可见性沿用 {@link BindingVisibility}
 * （会话不可见的技能不出现在结果中）。
 *
 * @since 1.0.0
 */
public class SkillSearchTool implements ToolCallback {

    static final int MAX_RESULTS = 20;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SkillRegistry registry;
    private final BindingVisibility visibility;

    public SkillSearchTool(SkillRegistry registry, SessionBindingIndex bindingIndex) {
        this.registry = registry;
        this.visibility = bindingIndex == null ? null : new BindingVisibility(registry, bindingIndex);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("skill_search")
                .description("按关键词检索可用技能（名称/描述子串匹配，不受目录注入上限限制）。"
                        + "返回匹配清单后可用 load_skill(name) 加载正文。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "query":{"type":"string","description":"检索关键词（子串，不分大小写）"}
                        },"required":["query"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String query;
        try {
            JsonNode args = MAPPER.readTree(toolInput == null || toolInput.isBlank() ? "{}" : toolInput);
            query = args.path("query").asText("");
        } catch (Exception e) {
            return "skill_search 参数解析失败：" + e.getMessage();
        }
        if (query.isBlank()) {
            return "skill_search 缺少 query 参数";
        }
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        String needle = query.toLowerCase(java.util.Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        int hits = 0;
        // 可见全集（listForPage 的 total 口径 = candidates 全量；检索不受注入上限限制）
        for (SkillMetadata meta : registry.listAllFor(null, null)) {
            if (visibility != null && !visibility.isVisible(sessionId, meta.name())) {
                continue;
            }
            boolean inName = meta.name() != null && meta.name().toLowerCase(java.util.Locale.ROOT).contains(needle);
            boolean inDesc = meta.description() != null
                    && meta.description().toLowerCase(java.util.Locale.ROOT).contains(needle);
            if (!inName && !inDesc) {
                continue;
            }
            if (hits == 0) {
                sb.append("匹配技能（").append(MAX_RESULTS).append(" 条上限）：\n");
            }
            hits++;
            sb.append("- ").append(meta.name());
            if (meta.description() != null && !meta.description().isBlank()) {
                sb.append(": ").append(meta.description());
            }
            sb.append('\n');
            if (hits >= MAX_RESULTS) {
                break;
            }
        }
        if (hits == 0) {
            return "无匹配技能（query=" + query + "）。可换更短的关键词，或请运维确认技能绑定关系。";
        }
        sb.append("用 load_skill(name) 加载正文。");
        return sb.toString();
    }
}
