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
            LOG.log(Level.WARNING, "Spill offload failed, passthrough original: " + uri, e);
            return new OffloadOutcome(toolResult, false, true, uri);
        }
        String preview = RangeReadEngine.previewOf(toolResult, previewChars, listPreviewItems);
        return new OffloadOutcome(placeholder(uri, toolResult.length(), preview), true, false, uri);
    }

    public RangeReadResult readBack(String spillUri, RangeReadRequest request) {
        return store.readRange(SpillUri.parse(spillUri), request);
    }

    private String placeholder(SpillUri uri, int sizeChars, String preview) {
        return """
                [工具结果过大，已溢出到磁盘]（共 %d 字符）
                预览：
                %s
                完整内容已保存至：%s
                完整内容请用 read_range 工具按需回读（请勿一次取回全文，按范围分次读取）：
                - 区间读取：read_range(path="%s", mode="bytes", offset=0, limit=20000)
                - JSON 字段抽取：read_range(path="%s", mode="json", jsonPath="$.xxx")
                - JSON 数组分页：read_range(path="%s", mode="page", limit=20)
                """.formatted(sizeChars, preview, uri, uri, uri, uri);
    }

    private String sanitize(String component) {
        return component.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
