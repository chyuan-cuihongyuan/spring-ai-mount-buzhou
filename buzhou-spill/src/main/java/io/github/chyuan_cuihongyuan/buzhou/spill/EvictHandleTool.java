package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * {@code evict_handle} 内置工具（wayfinder2 impl-16 / T44）：模型<b>主动</b>逐出已消费的
 * spill 句柄——上下文中对应的占位符在下一视图生成时替换为极简墓碑（Anthropic「清除已消费
 * tool_result 是最安全最轻的压缩」；跨 provider 由 harness 自持）。
 * 原文仍在 SpillStore，需要时可随时回读（逐出是视图优化、非数据删除）。
 */
public class EvictHandleTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HandleLifecycleRegistry registry;

    public EvictHandleTool(HandleLifecycleRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("evict_handle")
                .description("逐出已消费的溢出句柄（spill:// 路径）：其占位符将在下轮视图收缩为"
                        + "极简墓碑，为推理腾出上下文。原文仍持久保存、可随时回读。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"要逐出的 spill:// URI（来自占位符）"}
                        },"required":["path"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String path = args.path("path").asText();
            if (path == null || !path.startsWith("spill://")) {
                return "[逐出失败] 路径必须是 spill:// URI（收到：" + path + "）";
            }
            // 会话隔离：仅本会话的句柄可逐出（sessionId 经 ToolContext 注入）
            String scoped = HarnessToolCallingManager.sessionIdOf(toolContext) == null
                    ? path : path;
            registry.markEvicted(scoped);
            return "[已逐出] " + path + " 的占位符将在下一轮收缩为墓碑；原文仍可随时回读。";
        } catch (Exception e) {
            return "[逐出失败] 入参解析错误：" + e.getMessage();
        }
    }
}
