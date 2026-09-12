package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * span 状态分布读数（spec 712 / T1024，OpenTelemetry span status 语义）：
 * 对任意 span 集合聚合 (kind,status) 计数——错误率与 RUNNING 残留（开启后
 * 未关闭的 span，泄漏/崩溃残留信号）一屏可见。
 *
 * <p>纯函数读数（调用方供 spans——告警裁决归 312 订阅，存储归 store）。
 * 状态字符串不假设闭集（SpanStatus 之外的值照常聚合——前向兼容）。
 * errorRate 口径显式：ERROR / total（含 RUNNING/CANCELLED，无「终态率」歧义变体）。
 */
public final class SpanStatusDistribution {

    /** 单行聚合（kind+status 字典序——确定序可复现）。 */
    public record Row(String kind, String status, long count) {
    }

    /** 不可变报告。 */
    public record Report(List<Row> rows, long total, long runningResidue, double errorRate) {
    }

    private SpanStatusDistribution() {
    }

    /** 聚合（null fail-fast；空表 = 诚实零）。 */
    public static Report of(List<SpanRecord> spans) {
        Objects.requireNonNull(spans, "spans");
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        long errors = 0;
        long running = 0;
        for (SpanRecord span : spans) {
            if (span == null) {
                continue;
            }
            String kind = span.kind() == null ? "UNKNOWN" : span.kind();
            String status = span.status() == null ? "UNKNOWN" : span.status();
            counts.merge(kind + "|" + status, 1L, Long::sum);
            total++;
            if (SpanStatus.ERROR.equals(status)) {
                errors++;
            } else if (SpanStatus.RUNNING.equals(status)) {
                running++;
            }
        }
        List<Row> rows = new ArrayList<>(counts.size());
        counts.forEach((key, count) -> {
            int split = key.indexOf('|');
            rows.add(new Row(key.substring(0, split), key.substring(split + 1), count));
        });
        rows.sort(Comparator.comparing(Row::kind).thenComparing(Row::status));
        return new Report(List.copyOf(rows), total, running,
                total == 0 ? 0.0 : (double) errors / total);
    }
}
