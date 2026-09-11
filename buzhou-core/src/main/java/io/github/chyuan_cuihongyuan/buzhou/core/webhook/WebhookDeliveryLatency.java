package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * webhook 投递时延分位数（spec 514 / T777，spec 416 分位族同法——exact
 * 最近秩、零样本桶 null 诚实空值）：入队（createdAtEpochMs）→ 成功投递的
 * 时延样本滚动窗（有界 512），p50/p95/p99 + max/count 读数。「事件从产生
 * 到送达要多久」的分布面——135 lag 是积压滞后面，本面是已投递的时延分布
 * （两者互补：lag 看还没送的，这里看送出去花了多久）。
 *
 * <p>诚实边界：只记**成功投递**样本（死信/重试中不入——它们不是「送达」）；
 * 样本窗口有界（512）——重启清零（进程内观察面口径）。
 */
public final class WebhookDeliveryLatency {

    /** 默认样本窗容量。 */
    public static final int DEFAULT_WINDOW = 512;

    private final Deque<Long> samplesMillis = new ArrayDeque<>();
    private final int window;
    private long count;
    private long maxMillis;

    public WebhookDeliveryLatency() {
        this(DEFAULT_WINDOW);
    }

    public WebhookDeliveryLatency(int window) {
        if (window < 1) {
            throw new IllegalArgumentException("样本窗容量 >= 1（当前 " + window + "）");
        }
        this.window = window;
    }

    /** 记一次成功投递时延（毫秒；非正值忽略——时钟回拨防御）。 */
    public synchronized void record(long latencyMillis) {
        if (latencyMillis < 0) {
            return;
        }
        samplesMillis.addLast(latencyMillis);
        while (samplesMillis.size() > window) {
            samplesMillis.removeFirst();
        }
        count++;
        maxMillis = Math.max(maxMillis, latencyMillis);
    }

    /**
     * 分位数读数（exact 最近秩；零样本 = null 各分位——诚实空值不画零假象，
     * 416 同口径）。
     */
    public synchronized Snapshot snapshot() {
        if (samplesMillis.isEmpty()) {
            return new Snapshot(count, null, null, null, maxMillis == 0 ? null : maxMillis);
        }
        List sorted = new java.util.ArrayList<>(samplesMillis);
        java.util.Collections.sort(sorted);
        return new Snapshot(count,
                percentile(sorted, 50), percentile(sorted, 95), percentile(sorted, 99),
                maxMillis);
    }

    /** 窗内样本数。 */
    public synchronized int sampleCount() {
        return samplesMillis.size();
    }

    private static Long percentile(java.util.List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return null;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(sorted.size(), Math.max(1, rank)) - 1);
    }

    /** 时延读数（分位可 null=零样本）。 */
    public record Snapshot(long deliveredCount, Long p50Millis, Long p95Millis,
            Long p99Millis, Long maxMillis) {
    }
}
