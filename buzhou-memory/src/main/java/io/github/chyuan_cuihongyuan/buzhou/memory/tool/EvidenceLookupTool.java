package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
/** spec 1529 / T2309：证据查询工具——evidence-id 查询被微压缩回收的原始工具返回（占位符的证据回链）。 */

public class EvidenceLookupTool implements ToolCallback {

    // —— spec 1073 / impl 825：证据回查读面（Redis cache hit-rate 思想；静态面理由同
    // R46–R72 先例）。双守恒：calls = hits + misses；hits = completeReads + slicedReads。
    private static final java.util.concurrent.atomic.AtomicLong CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong MISSES =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong HITS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong COMPLETE_READS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong SLICED_READS =
            new java.util.concurrent.atomic.AtomicLong();

    /** 证据回查分布快照（spec 1073）。 */
    public record EvidenceLookupStats(long calls, long misses, long hits,
                                      long completeReads, long slicedReads) {
    }

    /** 只读快照（双守恒见类注）。 */
    public static EvidenceLookupStats stats() {
        return new EvidenceLookupStats(CALLS.get(), MISSES.get(), HITS.get(),
                COMPLETE_READS.get(), SLICED_READS.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        CALLS.set(0);
        MISSES.set(0);
        HITS.set(0);
        COMPLETE_READS.set(0);
        SLICED_READS.set(0);
    }

    private final MessageStore messageStore;

    public EvidenceLookupTool(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("read_evidence")
                .description("按 evidence-id 回查原始工具返回。可选 offset/limit 做字符区间范围读取。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "evidenceId":{"type":"string","description":"占位符中的 evidence-id"},
                          "offset":{"type":"integer","description":"起始字符偏移，默认 0"},
                          "limit":{"type":"integer","description":"最多返回字符数，默认全文"}
                        },"required":["evidenceId"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        CALLS.incrementAndGet();
        String evidenceId = extract(toolInput, "evidenceId");
        Integer offset = extractInt(toolInput, "offset");
        Integer limit = extractInt(toolInput, "limit");
        return messageStore.findById(evidenceId)
                .map(message -> rangeRead(message, offset, limit))
                .orElseGet(() -> {
                    MISSES.incrementAndGet();
                    return "未找到 evidence-id=" + evidenceId + " 对应的消息";
                });
    }

    private String rangeRead(BuzhouMessage message, Integer offset, Integer limit) {
        String content = message.content() == null ? "" : message.content();
        int start = offset == null ? 0 : Math.max(0, Math.min(offset, content.length()));
        int end = limit == null ? content.length() : Math.min(start + limit, content.length());
        HITS.incrementAndGet();
        String slice = content.substring(start, end);
        if (end < content.length()) {
            SLICED_READS.incrementAndGet();
            slice += "\n[已截断：" + (content.length() - end) + " 字符未返回，可调整 offset/limit 续读]";
        } else {
            COMPLETE_READS.incrementAndGet();
        }
        return slice;
    }

    private String extract(String toolInput, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(toolInput);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Integer extractInt(String toolInput, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*(\\d+)")
                .matcher(toolInput);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
