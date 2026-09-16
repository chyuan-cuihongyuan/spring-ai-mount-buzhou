package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 预热斜坡（spec 2044 / T3189 / impl 1595）——Guava warmup limiter
 * 思想：冷启动直接满速是洪峰——下游（新端点/新连接池/新模型路由）首
 * 秒被打挂；预热期速率从起始比例平滑爬升到满速（线性斜坡），warmup
 * 期满即恒满速。透传当前时刻的许可速率比例（0.0–1.0 乘数），由调用
 * 方作用在自身限流器/并发闸上。
 *
 * <p>纯函数式（时刻外注入——确定性可回放）；不可变。
 */
public final class WarmupRamp {

    /** 默认预热期（毫秒）。 */
    public static final long DEFAULT_WARMUP_MILLIS = 30_000L;

    /** 默认起始速率比例（10%——冷启动起步）。 */
    public static final double DEFAULT_START_FACTOR = 0.1d;

    private final long warmupMillis;
    private final double startFactor;

    /** 契约：warmupMillis &gt; 0、startFactor ∈ (0,1)（fail-fast）。 */
    public WarmupRamp(long warmupMillis, double startFactor) {
        if (warmupMillis <= 0) {
            throw new IllegalArgumentException("warmupMillis 须 > 0：" + warmupMillis);
        }
        if (!(startFactor > 0) || startFactor >= 1 || Double.isNaN(startFactor)) {
            throw new IllegalArgumentException("startFactor 须在 (0,1)：" + startFactor);
        }
        this.warmupMillis = warmupMillis;
        this.startFactor = startFactor;
    }

    public WarmupRamp() {
        this(DEFAULT_WARMUP_MILLIS, DEFAULT_START_FACTOR);
    }

    /**
     * elapsed 时刻的速率乘数 ∈ [startFactor, 1]：预热内线性爬升
     *（elapsed=0 恰 startFactor；=warmup 恰 1.0）；预热后恒 1.0。
     * 契约：elapsed ≥ 0（回拨视为 0——宽进）。
     */
    public double factorAt(long elapsedMillis) {
        if (elapsedMillis < 0) {
            elapsedMillis = 0; // 回拨宽进——按预热起点计
        }
        if (elapsedMillis >= warmupMillis) {
            return 1.0d;
        }
        return startFactor + (1.0d - startFactor) * (double) elapsedMillis / warmupMillis;
    }

    /** 预热是否已完成（factorAt 恒 1 的判定面）。 */
    public boolean warmedUp(long elapsedMillis) {
        return factorAt(elapsedMillis) >= 1.0d;
    }

    /** 起始比例读数（构造回显）。 */
    public double startFactor() {
        return startFactor;
    }

    /** 预热期读数（毫秒）。 */
    public long warmupMillis() {
        return warmupMillis;
    }
}
