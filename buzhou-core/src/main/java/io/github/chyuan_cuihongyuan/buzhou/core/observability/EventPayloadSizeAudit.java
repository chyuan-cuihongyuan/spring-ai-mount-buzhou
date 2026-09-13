package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 事件 payload 大小审计（spec 732 / T1062 族观测成本治理）：按事件类型聚合
 * payload 序列化字节——「哪类事件在吃观测存储/外发带宽」的治理证据面
 * （Sentry payload 限额、Loki 日志体量治理同思想）。
 *
 * <p>纯函数读数：大小=Jackson JSON 序列化字节数（与落盘/外发口径一致）；
 * 序列化失败的条目跳过（诚实计数）。
 */
public final class EventPayloadSizeAudit {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 单类型行（totalBytes 降序；同字节 type 字典序稳定）。 */
    public record Row(String type, long count, long totalBytes, long maxBytes) {
    }

    /** 不可变报告。 */
    public record Report(List<Row> rows, long totalBytes, int serialized, int skipped) {
    }

    private EventPayloadSizeAudit() {
    }

    /** 审计（null fail-fast；空表 = 零行）。 */
    public static Report analyze(List<EventRecord> events) {
        Objects.requireNonNull(events, "events");
        Map<String, long[]> byType = new LinkedHashMap<>(); // [0]=count [1]=total [2]=max
        long totalBytes = 0;
        int serialized = 0;
        int skipped = 0;
        for (EventRecord event : events) {
            if (event == null) {
                continue;
            }
            long bytes;
            try {
                bytes = MAPPER.writeValueAsBytes(event.payload()).length;
            } catch (Exception e) {
                skipped++; // 序列化失败诚实计数（payload 不可序列化=坏数据信号）
                continue;
            }
            String type = event.type() == null ? "UNKNOWN" : event.type();
            long[] agg = byType.computeIfAbsent(type, k -> new long[3]);
            agg[0]++;
            agg[1] += bytes;
            agg[2] = Math.max(agg[2], bytes);
            totalBytes += bytes;
            serialized++;
        }
        List<Row> rows = new ArrayList<>(byType.size());
        byType.forEach((type, agg) -> rows.add(new Row(type, agg[0], agg[1], agg[2])));
        rows.sort(Comparator.comparingLong(Row::totalBytes).reversed().thenComparing(Row::type));
        return new Report(List.copyOf(rows), totalBytes, serialized, skipped);
    }
}
