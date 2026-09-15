package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 情节保留普查（L 会话 1700 系 R27 = effort #1726 / spec 1726 /
 * 票 T2653 + T2654 / impl 1326）——Kafka log segment retention 思想：
 * {@link EpisodeLedger} 的情节保留/逐出（容量压力/TTL 到期）无分布——
 * 「情节是被时间赶走还是被新情节挤走」决定保留策略调参方向。
 *
 * <p>实例面线程安全：`RetentionEvent` 闭集（STORED 写入 / EVICTED_TTL
 * 到期逐出 / EVICTED_CAPACITY 容量逐出）+record(n)+census+逐出占比
 * （无样本 −1）+resetForTest。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class EpisodeRetentionStats {

    /** 保留事件闭集。 */
    public enum RetentionEvent { STORED, EVICTED_TTL, EVICTED_CAPACITY }

    private final Map<RetentionEvent, AtomicLong> counters = new EnumMap<>(RetentionEvent.class);

    /** 默认构造。 */
    public EpisodeRetentionStats() {
        for (RetentionEvent event : RetentionEvent.values()) {
            counters.put(event, new AtomicLong());
        }
    }

    /** 记 n 起保留事件（批量逐出一次记多条；n&lt;0 忽略）。 */
    public void record(RetentionEvent event, int n) {
        if (n > 0) {
            counters.get(event).addAndGet(n);
        }
    }

    /**
     * @param stored          累计写入数
     * @param evictedTtl      TTL 到期逐出数
     * @param evictedCapacity 容量逐出数
     * @param evictRatio      逐出占比 (ttl+capacity)/(stored+逐出)；无样本 −1
     */
    public record RetentionCensus(long stored, long evictedTtl,
                                  long evictedCapacity, double evictRatio) {
    }

    /** 快照。 */
    public RetentionCensus census() {
        long stored = counters.get(RetentionEvent.STORED).get();
        long ttl = counters.get(RetentionEvent.EVICTED_TTL).get();
        long cap = counters.get(RetentionEvent.EVICTED_CAPACITY).get();
        long total = stored + ttl + cap;
        double ratio = total == 0 ? -1d : (double) (ttl + cap) / total;
        return new RetentionCensus(stored, ttl, cap, ratio);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}
