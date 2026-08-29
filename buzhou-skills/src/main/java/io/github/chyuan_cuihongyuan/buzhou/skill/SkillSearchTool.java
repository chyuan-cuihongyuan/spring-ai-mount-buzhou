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
 * <p><b>语义排序与近邻提示（spec 73 §A / T297 / effort#33）</b>：注入语义排序器
 * （semantic-ranking.enabled）时——命中集按与 query 的 cosine 相似度排序（预算内保
 * 最相关，#19 同款 ranker 复用）；<b>零子串命中时</b>给语义最近 3 条「或许你要找」
 * 提示（无阈值判定——排序质量归嵌入模型，诚实边界与 spec 59 同口径）。
 *
 * @since 1.0.0
 */
public class SkillSearchTool implements ToolCallback {

    static final int MAX_RESULTS = 20;
    /** 零子串命中时的语义近邻提示条数。 */
    static final int SEMANTIC_SUGGESTIONS = 3;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SkillRegistry registry;
    private final BindingVisibility visibility;
    /** spec 73 §A / T297：语义排序器（null = 纯子串检索，行为与历史一致）。 */
    private final SemanticSkillRanker ranker;

    public SkillSearchTool(SkillRegistry registry, SessionBindingIndex bindingIndex) {
        this(registry, bindingIndex, null);
    }

    /** spec 73 §A / T297：带语义排序的构造（与目录注入共享同一 ranker 实例）。 */
    public SkillSearchTool(SkillRegistry registry, SessionBindingIndex bindingIndex,
            SemanticSkillRanker ranker) {
        this.registry = registry;
        this.visibility = bindingIndex == null ? null : new BindingVisibility(registry, bindingIndex);
        this.ranker = ranker;
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

        java.util.List<SkillMetadata> visible = new java.util.ArrayList<>();
        java.util.List<SkillMetadata> matched = new java.util.ArrayList<>();
        for (SkillMetadata meta : registry.listAllFor(null, null)) {
            if (visibility != null && !visibility.isVisible(sessionId, meta.name())) {
                continue;
            }
            visible.add(meta);
            boolean inName = meta.name() != null && meta.name().toLowerCase(java.util.Locale.ROOT).contains(needle);
            boolean inDesc = meta.description() != null
                    && meta.description().toLowerCase(java.util.Locale.ROOT).contains(needle);
            if (inName || inDesc) {
                matched.add(meta);
            }
        }
        if (matched.isEmpty()) {
            // spec 73 §A / T297：零子串命中 → 语义近邻提示（无阈值——排序质量归嵌入模型）
            if (ranker != null && !visible.isEmpty()) {
                java.util.List<SkillMetadata> nearest = ranker.rank(visible, query);
                StringBuilder suggest = new StringBuilder("无子串匹配（query=" + query + "）。语义最近：\n");
                int shown = 0;
                for (SkillMetadata meta : nearest) {
                    if (shown >= SEMANTIC_SUGGESTIONS) {
                        break;
                    }
                    shown++;
                    suggest.append("- ").append(meta.name());
                    if (meta.description() != null && !meta.description().isBlank()) {
                        suggest.append(": ").append(meta.description());
                    }
                    suggest.append('\n');
                }
                if (shown > 0) {
                    suggest.append("可换更精确的关键词，或直接 load_skill(name) 加载上述技能。");
                    searchTelemetry("miss-semantic");
                    return suggest.toString();
                }
            }
            searchTelemetry("miss");
            return "无匹配技能（query=" + query + "）。可换更短的关键词，或请运维确认技能绑定关系。";
        }
        // spec 73 §A / T297：命中集语义排序（相关在前；失败回退注册序——ranker 内建）
        java.util.List<SkillMetadata> ordered = ranker == null ? matched : ranker.rank(matched, query);

        StringBuilder sb = new StringBuilder();
        sb.append("匹配技能（").append(MAX_RESULTS).append(" 条上限）：\n");
        int hits = 0;
        for (SkillMetadata meta : ordered) {
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
        sb.append("用 load_skill(name) 加载正文。");
        searchTelemetry("hit");
        return sb.toString();
    }

    /** spec 116 §A / T413：检索遥测（outcome=hit|miss|miss-semantic——命中率即技能可发现性信号）。 */
    private static void searchTelemetry(String outcome) {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.skills.search", "outcome", outcome);
    }
}
