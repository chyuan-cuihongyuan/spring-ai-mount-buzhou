package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 评估 run 项耗时分布（spec 544 / T837——108/111 timer 面的分布化扩散；
 * 416 分位族同法）：run 内各项 durationMs 的 exact 最近秩 p50/p95 + 最慢
 * 项 top（itemId+耗时）——「整个 run 慢在哪一项」一屏可读。
 *
 * <p>诚实边界：纯函数读数（跑内分布，不做跨 run 趋势——JSONL 下游）。
 */
public final class EvalRunDurationStats {

    /** 最慢项行。 */
    public record SlowestItem(String itemId, long durationMs) {
    }

    /** 分布读数（p50/p95 可 null=零样本）。 */
    public record DurationStats(int items, Long p50Millis, Long p95Millis,
            long maxMillis, List<SlowestItem> slowest) {
    }

    private EvalRunDurationStats() {
    }

    /** run 内项耗时分布。 */
    public static DurationStats analyze(EvalRunResult run) {
        if (run == null) {
            throw new IllegalArgumentException("run 必须非空");
        }
        List<Long> durations = new ArrayList<>();
        for (EvalRunItemResult item : run.items()) {
            durations.add(item.durationMs());
        }
        durations.sort(Comparator.naturalOrder());
        Long p50 = durations.isEmpty() ? null : percentile(durations, 50);
        Long p95 = durations.isEmpty() ? null : percentile(durations, 95);
        long max = durations.isEmpty() ? 0 : durations.get(durations.size() - 1);

        List<SlowestItem> slowest = new ArrayList<>(run.items().stream()
                .map(i -> new SlowestItem(i.itemId(), i.durationMs()))
                .sorted(Comparator.comparingLong(SlowestItem::durationMs).reversed()
                        .thenComparing(SlowestItem::itemId))
                .limit(3)
                .toList());
        return new DurationStats(run.items().size(), p50, p95, max, slowest);
    }

    private static Long percentile(List<Long> sorted, double p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(sorted.size(), Math.max(1, rank)) - 1);
    }
}
