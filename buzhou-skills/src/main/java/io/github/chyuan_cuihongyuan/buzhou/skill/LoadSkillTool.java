package io.github.chyuan_cuihongyuan.buzhou.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Optional;

/**
 * {@code load_skill} 内置原子工具（spec 04）。
 *
 * <p>按名返回 Skill 正文（Markdown 原文）+ 资源清单。入参 {@code name} 取自系统提示词中的
 * Skill Catalog。失败转文本（不抛异常中断循环，同 06 号档工具失败语义）；不存在/未绑定返回
 * 指引文本。
 *
 * <p>入参校验（spec 04）：name 必须在当前会话绑定清单内——sessionId 取自 ToolContext 的
 * {@link HarnessToolCallingManager#SESSION_ID_KEY}，经 {@link SessionBindingIndex} 反查绑定后
 * 以 {@link SkillRegistry#isVisibleFor} 判定；非会话内直调（无 sessionId）不校验。
 * 解析按名（DB-PUBLISHED &gt; classpath）。资源按需读取复用 {@code read_range}，
 * 路径约定 {@code skill://<name>/<relativePath>}。
 */
public class LoadSkillTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SkillRegistry registry;
    private final BindingVisibility visibility;

    public LoadSkillTool(SkillRegistry registry, SessionBindingIndex bindingIndex) {
        this.registry = registry;
        this.visibility = bindingIndex == null ? null : new BindingVisibility(registry, bindingIndex);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("load_skill")
                .description("按名加载技能正文（Markdown 原文）与资源清单。name 取自系统提示词中的 Skill Catalog。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "name":{"type":"string","description":"技能名称（Skill Catalog 清单首列）"}
                        },"required":["name"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String name;
        try {
            JsonNode args = MAPPER.readTree(toolInput == null || toolInput.isBlank() ? "{}" : toolInput);
            name = args.path("name").asText("");
        } catch (Exception e) {
            return "load_skill 参数解析失败：" + e.getMessage();
        }
        if (name.isBlank()) {
            return "load_skill 缺少 name 参数";
        }
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        if (visibility != null && !visibility.isVisible(sessionId, name)) {
            return "技能不存在或未绑定：" + name;
        }
        // 按名解析（绑定校验已在上方完成；appId/agentName 对解析本身无影响）
        Optional<Skill> skill = registry.load(null, null, name);
        if (skill.isEmpty()) {
            return "技能不存在或未绑定：" + name;
        }
        return formatResult(skill.get());
    }

    private String formatResult(Skill skill) {
        StringBuilder sb = new StringBuilder();
        if (skill.body() != null && !skill.body().isBlank()) {
            sb.append(skill.body());
        } else {
            sb.append("（技能 ").append(skill.name()).append(" 无正文）");
        }
        if (!skill.resources().isEmpty()) {
            sb.append("\n\n## 资源清单（按需调 read_range 取内容，路径 skill://")
                    .append(skill.name()).append("/<relativePath>）\n");
            for (SkillResource r : skill.resources()) {
                sb.append("- ").append(r.relativePath())
                        .append(" (").append(r.sizeBytes()).append(" bytes, ")
                        .append(r.mediaType()).append(")\n");
            }
        }
        return sb.toString().strip();
    }
}
