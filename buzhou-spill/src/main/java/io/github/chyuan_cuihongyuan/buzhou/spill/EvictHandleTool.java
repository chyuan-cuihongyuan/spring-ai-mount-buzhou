package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code evict_handle} 内置工具（wayfinder2 impl-16 / T44）：模型<b>主动</b>逐出已消费的
 * spill 句柄——上下文中对应的占位符在下一视图生成时替换为极简墓碑（Anthropic「清除已消费
 * tool_result 是最安全最轻的压缩」；跨 provider 由 harness 自持）。
 * 原文仍在 SpillStore，需要时可随时回读（逐出是视图优化、非数据删除）。
 */
public class EvictHandleTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HandleLifecycleRegistry registry;

    // —— spec 1053 / impl 805：逐出判定读面（Anthropic tool_result 清除采用率思想；
    // 静态面理由同 R46–R52 先例）。守恒：attempts = evictions + 两拒绝桶之和。
    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong EVICTIONS = new AtomicLong();
    private static final AtomicLong BAD_PATH_REJECTS = new AtomicLong();
    private static final AtomicLong PARSE_REJECTS = new AtomicLong();

    /** 逐出判定分布快照（spec 1053）。 */
    public record EvictStats(long attempts, long evictions,
                             long badPathRejects, long parseRejects) {
    }

    /** 只读快照（守恒 attempts = evictions + badPathRejects + parseRejects）。 */
    public static EvictStats stats() {
        return new EvictStats(ATTEMPTS.get(), EVICTIONS.get(),
                BAD_PATH_REJECTS.get(), PARSE_REJECTS.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        EVICTIONS.set(0);
        BAD_PATH_REJECTS.set(0);
        PARSE_REJECTS.set(0);
    }

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
        ATTEMPTS.incrementAndGet();
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String path = args.path("path").asText();
            if (path == null || !path.startsWith("spill://")) {
                BAD_PATH_REJECTS.incrementAndGet();
                return "[逐出失败] 路径必须是 spill:// URI（收到：" + path + "）";
            }
            // 会话隔离：仅本会话的句柄可逐出（sessionId 经 ToolContext 注入）
            String scoped = HarnessToolCallingManager.sessionIdOf(toolContext) == null
                    ? path : path;
            registry.markEvicted(scoped);
            EVICTIONS.incrementAndGet();
            return "[已逐出] " + path + " 的占位符将在下一轮收缩为墓碑；原文仍可随时回读。";
        } catch (Exception e) {
            PARSE_REJECTS.incrementAndGet();
            return "[逐出失败] 入参解析错误：" + e.getMessage();
        }
    }
}
