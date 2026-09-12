package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 会话状态 TTL 覆盖审计（spec 724 / T1048，S3/MinIO 生命周期审计思想）：
 * 永生键（ttlTurns=null）持续堆积是状态存储膨胀主通道——本面给出覆盖
 * 率与 producer 归因（哪个机制在写永生键）。纯读数（补 TTL 动作归宿主）。
 */
public final class StateTtlCoverage {

    /** 单 producer 行（persistent=永生键数——治理焦点）。 */
    public record Row(String producer, long keys, long persistent, long ttlKeys) {
    }

    /** 不可变报告（coverage=ttlKeys/totalKeys；total=0 空真 1.0）。 */
    public record Report(long totalKeys, long persistentKeys, long ttlKeys,
                         double coverage, List<Row> byProducer) {
    }

    private StateTtlCoverage() {
    }

    /** 审计（null fail-fast；空 map = 空真全 0 + coverage 1.0）。 */
    public static Report analyze(Map<String, StateEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        long total = 0;
        long persistent = 0;
        Map<String, long[]> byProducer = new TreeMap<>(); // [0]=keys [1]=persistent
        for (Map.Entry<String, StateEntry> e : entries.entrySet()) {
            StateEntry entry = e.getValue();
            if (entry == null) {
                continue;
            }
            total++;
            String producer = entry.producer() == null ? "unknown" : entry.producer();
            boolean eternal = entry.ttlTurns() == null;
            if (eternal) {
                persistent++;
            }
            long[] agg = byProducer.computeIfAbsent(producer, k -> new long[2]);
            agg[0]++;
            if (eternal) {
                agg[1]++;
            }
        }
        List<Row> rows = new ArrayList<>(byProducer.size());
        byProducer.forEach((producer, agg) -> rows.add(new Row(producer, agg[0], agg[1], agg[0] - agg[1])));
        rows.sort(Comparator.comparing(Row::producer));
        double coverage = total == 0 ? 1.0 : (double) (total - persistent) / total;
        return new Report(total, persistent, total - persistent, coverage, List.copyOf(rows));
    }
}
