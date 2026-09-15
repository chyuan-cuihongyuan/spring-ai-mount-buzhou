package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 幂等键冲突读面（L 会话 1700 系 R48 = effort #1747 / spec 1747 /
 * 票 T2695 + T2696 / impl 1348）——Stripe 幂等键（Idempotency-Key）遥测
 * 思想：{@link IdempotencyAdvisor} 用幂等键去重重试，但键重复命中
 * （同键二次到达=真重试；跨场景复用=键生成缺陷）无账——冲突占比异常
 * 升高 = 键生成有问题，不是真重试。
 *
 * <p>实例面线程安全：`record(key, replayed)`——replayed=true 表示同键
 * 二次到达；distinct 键集有界（默认 256 FIFO 逐出）；census 吐
 * distinctKeys/totalRecords/collisionRatio（replayed 占比，无样本 −1）。
 * 纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class IdempotencyCollisions {

    /** 默认键基数上限。 */
    public static final int DEFAULT_MAX_KEYS = 256;

    private final int maxKeys;
    private final Object lock = new Object();
    private final Set<String> distinct = new LinkedHashSet<>();
    private long totalRecords;
    private long replayedRecords;

    /** 默认容量。 */
    public IdempotencyCollisions() {
        this(DEFAULT_MAX_KEYS);
    }

    /** 自定义键基数上限（&lt;1 按默认）。 */
    public IdempotencyCollisions(int maxKeys) {
        this.maxKeys = maxKeys < 1 ? DEFAULT_MAX_KEYS : maxKeys;
    }

    /** 记一次幂等键到达（replayed=true = 同键二次到达）。 */
    public void record(String key, boolean replayed) {
        String normalized = key == null || key.isBlank() ? "_blank_" : key;
        synchronized (lock) {
            totalRecords++;
            if (replayed) {
                replayedRecords++;
            }
            distinct.add(normalized);
            while (distinct.size() > maxKeys) {
                java.util.Iterator<String> it = distinct.iterator();
                it.next();
                it.remove();
            }
        }
    }

    /**
     * @param distinctKeys   不同键数（≤容量）
     * @param totalRecords   到达总数
     * @param replayedRecords 重放到达数
     * @param collisionRatio  重放占比 replayed/total；无样本哨兵 −1
     */
    public record CollisionCensus(int distinctKeys, long totalRecords,
                                  long replayedRecords, double collisionRatio) {
    }

    /** 快照。 */
    public CollisionCensus census() {
        synchronized (lock) {
            double ratio = totalRecords == 0 ? -1d
                    : (double) replayedRecords / totalRecords;
            return new CollisionCensus(distinct.size(), totalRecords, replayedRecords, ratio);
        }
    }
}
