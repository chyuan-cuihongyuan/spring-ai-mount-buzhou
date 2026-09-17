package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.random.RandomGenerator;

/**
 * Morris 近似计数器（spec 3008 / T5017 / impl 2009）——Robert
 * Morris 1978 思想：**概率计数换空间**——只存指数 v，增量以 2^−v
 * 概率才 +1（计数越大越懒），估计 = 2^v − 1，期望 E[estimate] = n
 * （无偏）；空间 O(log log n) 位——十亿级计数 5 位指数足够。海量
 * 低价值基数（调用次数/心跳数/提及数）的粗口径件：单计数 8 字节
 * 换十亿量级 ±50% 相对误差量级——精确计数该用 long，本件换的是
 * 空间与写放大（计数热点免每命中写共享行）。
 *
 * <p>RandomGenerator 注入（确定性回放）；指数封顶 62（double 概率
 * 分辨率与 long 估计饱和的诚实边界——覆盖 ~4.6e18 计数）。
 */
public final class MorrisCounter {

    /** 算法基数（概率 2^−v / 估计 2^v−1）。 */
    private static final int BASE = 2;

    /** 指数封顶（2^62 恰在 double 概率分辨率与 long 饱和内）。 */
    private static final int MAX_EXPONENT = 62;

    private final RandomGenerator rng;
    private int exponent;

    /** 注入随机源（确定性回放/生产各异）。 */
    public MorrisCounter(RandomGenerator rng) {
        this.rng = rng;
    }

    /**
     * 概率增量：以 2^−v 概率指数 +1（v=0 必增——首计数不丢）；
     * 封顶后不再增（饱和诚实）。
     */
    public void increment() {
        if (exponent >= MAX_EXPONENT) {
            return;
        }
        if (rng.nextDouble() < 1.0 / (1L << exponent)) {
            exponent++;
        }
    }

    /** 估计计数 2^v − 1（期望无偏；v=0 → 0 诚实零）。 */
    public long estimate() {
        return (1L << exponent) - 1;
    }

    /** 原始指数读数（对账/测试面）。 */
    public int rawExponent() {
        return exponent;
    }

    /** 归零。 */
    public void reset() {
        exponent = 0;
    }
}
