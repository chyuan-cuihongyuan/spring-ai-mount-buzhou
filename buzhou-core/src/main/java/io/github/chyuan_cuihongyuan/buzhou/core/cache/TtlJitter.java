package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.DeterministicHash;

/**
 * TTL 确定性抖动（spec 3019 / T5039 / impl 2020）——缓存防雷群
 * 思想（memcached jitter / AWS 架构博客实践）：同批写入的键若同
 * TTL 同时到期，到期瞬间的集体回源（cache stampede/雷群）打爆
 * 源站；**按键确定性抖动**——TTL = base×(1 + r·jitter)，r ∈[−1,1)
 * 由键哈希派生：同一键跨实例跨重启恒同 TTL（副本一致——各副本
 * 过期时刻天然错开，且抖动无需共享随机源可复算），不同键在带宽
 * 内铺开到期时刻。
 *
 * <p>纯函数静态件；带宽 [base×(1−ratio), base×(1+ratio)]，下限
 * 兜底 1ms；ratio∈[0,1)、base ≥ 1 校验。
 */
public final class TtlJitter {

    /** TTL 下限兜底（毫秒——再强的抖动不过期即刻）。 */
    public static final long MIN_TTL_MILLIS = 1;

    private TtlJitter() {
    }

    /**
     * 键确定性抖动 TTL：r = hash(key) 归一 [−1,1)，ttl =
     * round(base×(1 + r×jitter))，夹带宽且兜底 1ms。
     */
    public static long jitteredTtlMillis(long baseMillis, double jitterRatio, String key) {
        if (baseMillis < MIN_TTL_MILLIS) {
            throw new IllegalArgumentException("baseMillis ≥ 1：" + baseMillis);
        }
        if (!(jitterRatio >= 0 && jitterRatio < 1)) {
            throw new IllegalArgumentException("jitterRatio ∈ [0,1)：" + jitterRatio);
        }
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        long hash = DeterministicHash.hash64(key);
        double unit = (hash >>> 1) / (double) Long.MAX_VALUE; // [0,1]（63 位无符号归一）
        double r = unit * 2 - 1;                             // [−1,1]（边界由带宽夹持兜住）
        long ttl = Math.round(baseMillis * (1 + r * jitterRatio));
        long floor = Math.max(MIN_TTL_MILLIS, Math.round(baseMillis * (1 - jitterRatio)));
        long ceiling = Math.round(baseMillis * (1 + jitterRatio));
        return Math.max(floor, Math.min(ceiling, ttl));
    }
}
