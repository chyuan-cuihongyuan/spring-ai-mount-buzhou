package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class SpillService {

    private static final Logger LOG = System.getLogger(SpillService.class.getName());

    private final SpillStore store;
    private final int previewChars;
    private final int listPreviewItems;

    public SpillService(SpillStore store, int previewChars, int listPreviewItems) {
        this.store = store;
        this.previewChars = previewChars;
        this.listPreviewItems = listPreviewItems;
    }

    public record OffloadOutcome(String text, boolean offloaded, boolean degraded, SpillUri uri) {
    }

    public String offloadIfNeeded(String agentName, String sessionId, String toolCallId,
                                  String toolName, String toolResult, int thresholdChars) {
        return tryOffload(agentName, sessionId, toolCallId, toolName, toolResult, thresholdChars)
                .text();
    }

    public OffloadOutcome tryOffload(String agentName, String sessionId, String toolCallId,
                                     String toolName, String toolResult, int thresholdChars) {
        if (toolResult == null || toolResult.length() < thresholdChars) {
            return new OffloadOutcome(toolResult, false, false, null);
        }
        SpillUri uri = new SpillUri(sanitize(agentName), sanitize(sessionId), sanitize(toolCallId));
        try {
            store.store(SpillEntry.of(uri, toolResult), previewChars);
        } catch (RuntimeException e) {
            // 视图级溢出（HotTail）每个注入视图都会重试同一 callId：同内容已落盘 → 幂等复用占位符
            //（保留 DiskSpillStore「一次调用一次落盘」对<b>不同</b>内容的守卫语义）
            if (isAlreadyStored(uri, toolResult)) {
                String preview = RangeReadEngine.previewOf(toolResult, previewChars, listPreviewItems);
                return new OffloadOutcome(placeholder(uri, toolResult, preview,
                        preview.length() < toolResult.length()), true, false, uri);
            }
            LOG.log(Level.WARNING, "Spill offload failed, passthrough original: " + uri, e);
            return new OffloadOutcome(toolResult, false, true, uri);
        }
        String preview = RangeReadEngine.previewOf(toolResult, previewChars, listPreviewItems);
        boolean previewTruncated = preview.length() < toolResult.length();
        return new OffloadOutcome(placeholder(uri, toolResult, preview, previewTruncated),
                true, false, uri);
    }

    public RangeReadResult readBack(String spillUri, RangeReadRequest request) {
        return store.readRange(SpillUri.parse(spillUri), request);
    }

    /** 同一 URI 已存且内容一致（视图级重入的幂等判定）。 */
    private boolean isAlreadyStored(SpillUri uri, String content) {
        try {
            return store.exists(uri) && java.util.Objects.equals(store.load(uri).orElse(null), content);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 自描述占位符（wayfinder T20 / docs/spec/11 spill）：句柄 + 数据形状/schema 提示 +
     * 字节/token 大小 + 精确回读动词与参数——取代裸路径（arXiv pointer-offloading + MCP results-widget）。
     * 任何截断（预览）必发显式标记，永不静默。
     */
    private String placeholder(SpillUri uri, String content, String preview, boolean previewTruncated) {
        int sizeChars = content.length();
        int estTokens = (sizeChars + 3) / 4; // 4 字符/token 启发式（与 core CharHeuristicTokenEstimator 一致）
        String previewNote = previewTruncated ? "（预览已截断，仅前 %d/%d 字符，全文请回读）"
                .formatted(preview.length(), sizeChars) : "（全文）";
        return """
                [工具结果过大，已溢出]（自描述句柄，按需回读）
                句柄：%s
                数据形状：%s
                大小：%,d 字符（约 %,d token，按 4 字符/token 估算）
                预览%s：
                %s
                完整内容请用 read_range 工具按需回读（请勿一次取回全文，按范围分次读取）：
                - 区间读取：read_range(path="%s", mode="bytes", offset=0, limit=20000)
                - JSON 字段抽取：read_range(path="%s", mode="json", jsonPath="$.xxx")
                - JSON 数组分页：read_range(path="%s", mode="page", limit=20)
                """.formatted(uri, shapeOf(content), sizeChars, estTokens, previewNote, preview,
                uri, uri, uri);
    }

    /** 数据形状提示：JSON 数组/对象给结构线索，其余按文本行数描述。 */
    static String shapeOf(String content) {
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
                if (node.isArray()) {
                    return "JSON 数组（" + node.size() + " 项；建议 mode=\"page\" 分页或 mode=\"json\" jsonPath 取字段）";
                }
            } catch (Exception ignored) {
            }
        }
        if (trimmed.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
                if (node.isObject()) {
                    java.util.List<String> fields = new java.util.ArrayList<>();
                    node.fieldNames().forEachRemaining(fields::add);
                    String shown = String.join(", ", fields.size() > 8 ? fields.subList(0, 8) : fields);
                    String suffix = fields.size() > 8 ? "…共 " + fields.size() + " 个" : "";
                    return "JSON 对象（顶层字段：" + shown + suffix + "）";
                }
            } catch (Exception ignored) {
            }
        }
        long lines = content.lines().count();
        return "文本（" + lines + " 行；建议 mode=\"bytes\" 区间读取）";
    }

    private String sanitize(String component) {
        return component.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
