package io.github.chyuan_cuihongyuan.buzhou.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记忆压缩规模分布观测（spec 517 / T777，spec 416 分位族同法）：挂在既有
 * CompactionListener 缝上的有界样本窗——微压缩回收字符（p50/p95/max）+
 * 逐出比直方图（0.8/0.9/1.0 梯子级）+ 摘要折入 trigger 计数。「压缩实际
 * 回收了多少、逐出加压到哪级、摘要折叠由什么触发」一屏可答（204 fold
 * 计数的分布化深挖）。
 *
 * <p>诚实边界：只观测不干预（压缩行为零变化）；样本窗有界（512/128）——
 * 重启清零（进程内观察面口径）。
 */
public final class CompactionRatioStats {

    /** 默认微压缩样本窗。 */
    public static final int DEFAULT_WINDOW = 512;
    /** 默认折入样本窗。 */
    public static final int DEFAULT_FOLD_WINDOW = 128;

    private final Deque<Long> reclaimedSamples = new ArrayDeque<>();
    private final Deque<Long> foldSamples = new ArrayDeque<>();
    private final int window;
    private final int foldWindow;
    private final Map<Double, AtomicLong> evictRatioHistogram =
            new LinkedHashMap<>(Map.of(0.8d, new AtomicLong(), 0.9d, new AtomicLong(), 1.0d, new AtomicLong()));
    private final Map<String, AtomicLong> triggerCounts = new LinkedHashMap<>();
    private final AtomicLong totalReclaimed = new AtomicLong();
    private final AtomicLong totalFolds = new AtomicLong();

    public CompactionRatioStats() {
        this(DEFAULT_WINDOW, DEFAULT_FOLD_WINDOW);
    }

    public CompactionRatioStats(int window, int foldWindow) {
        if (window < 1 || foldWindow < 1) {
            throw new IllegalArgumentException("样本窗容量 >= 1");
        }
        this.window = window;
        this.foldWindow = foldWindow;
    }

    /** 记一次微压缩（回收字符 + 逐出比）。 */
    public synchronized void recordCompaction(int reclaimedChars, double evictRatio) {
        if (reclaimedChars < 0) {
            return;
        }
        reclaimedSamples.addLast((long) reclaimedChars);
        while (reclaimedSamples.size() > window) {
            reclaimedSamples.removeFirst();
        }
        totalReclaimed.addAndGet(reclaimedChars);
        evictRatioHistogram.computeIfAbsent(evictRatio, k -> new AtomicLong()).incrementAndGet();
    }

    /** 记一次摘要折入（折入后摘要字符规模 + 触发判据）。 */
    public synchronized void recordFold(int summaryChars, String trigger) {
        if (summaryChars < 0 || trigger == null || trigger.isEmpty()) {
            return;
        }
        totalFolds.incrementAndGet();
        triggerCounts.computeIfAbsent(trigger, k -> new AtomicLong()).incrementAndGet();
        // 折入样本独立窗（与回收字符分布分开——两列语义不互染）
        foldSamples.addLast((long) summaryChars);
        while (foldSamples.size() > foldWindow) {
            foldSamples.removeFirst();
        }
    }

    /** 分布读数（exact 最近秩 p50/p95——零样本 null，416 同口径）。 */
    public synchronized Snapshot snapshot() {
        return new Snapshot(
                totalReclaimed.get(),
                percentile(reclaimedSamples, 50),
                percentile(reclaimedSamples, 95),
                histogramSnapshot(evictRatioHistogram),
                totalFolds.get(),
                percentile(foldSamples, 50),
                triggerCountsSnapshot());
    }

    private static Long percentile(Deque<Long> sortedSource, double p) {
        if (sortedSource.isEmpty()) {
            return null;
        }
        List<Long> sorted = new java.util.ArrayList<>(sortedSource);
        java.util.Collections.sort(sorted);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(sorted.size(), Math.max(1, rank)) - 1);
    }

    private static Map<Double, Long> histogramSnapshot(Map<Double, AtomicLong> histogram) {
        Map<Double, Long> out = new LinkedHashMap<>();
        histogram.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    private static Map<String, Long> triggerCountsSnapshot(Map<String, AtomicLong> counts) {
        Map<String, Long> out = new LinkedHashMap<>();
        counts.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    private Map<String, Long> triggerCountsSnapshot() {
        return triggerCountsSnapshot(triggerCounts);
    }

    /** 分布读数（p50/p95 可 null=零样本）。 */
    public record Snapshot(long totalReclaimedChars, Long p50ReclaimedChars,
            Long p95ReclaimedChars, Map<Double, Long> evictRatioHistogram,
            long totalFolds, Long p50FoldChars, Map<String, Long> foldTriggerCounts) {
    }
}
