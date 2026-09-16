package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 香农熵读数（spec 2050 / T3201 / impl 1601）——信息论经典（Shannon
 * 1948）思想：分布多样性的统一量纲——H = −Σp·log p：全集中单点 = 0
 *（零多样性）、均匀 k 类 = log k（最大多样性）。评估答案分布多样
 * 性 / 路由分布集中度 / 工具调用分布的「偏斜度」有连续量纲可比（比
 * 卡方二值判定更细）。
 *
 * <p>纯函数零状态、确定性；底可选（2=bits / e=nats / 10=dits）。
 */
public final class ShannonEntropy {

    /** 常用底：2（bits）。 */
    public static final double BASE_2 = 2.0d;

    /** 常用底：e（nats）。 */
    public static final double BASE_E = Math.E;

    private ShannonEntropy() {
    }

    /**
     * 频数分布的熵：H = −Σ(nᵢ/N)·log(nᵢ/N)。零频类不计（0·log0 = 0
     * 惯例）。契约：counts 非空、非负、总量 &gt; 0、base &gt; 1
     *（fail-fast——base 1 退化）。
     */
    public static double entropy(long[] counts, double base) {
        if (counts == null || counts.length == 0) {
            throw new IllegalArgumentException("counts 不能为空");
        }
        if (!(base > 1) || Double.isNaN(base)) {
            throw new IllegalArgumentException("base 须 > 1：" + base);
        }
        long total = Arrays.stream(counts).sum();
        if (total <= 0) {
            throw new IllegalArgumentException("总量须 > 0");
        }
        double h = 0.0d;
        for (long c : counts) {
            if (c < 0) {
                throw new IllegalArgumentException("频数须 ≥ 0：" + c);
            }
            if (c == 0) {
                continue; // 0·log0 = 0
            }
            double p = (double) c / total;
            h -= p * (Math.log(p) / Math.log(base));
        }
        return h;
    }

    /** bits 便捷口径。 */
    public static double entropyBits(long[] counts) {
        return entropy(counts, BASE_2);
    }

    /**
     * 归一化熵 ∈ [0,1]：H ÷ log₂(k)（k = 非零类数——零频类不贡献上界）。
     * 全集中 0、均匀 1——跨分布直接可比。
     */
    public static double normalizedEntropy(long[] counts) {
        if (counts == null || counts.length == 0) {
            throw new IllegalArgumentException("counts 不能为空");
        }
        long nonZero = Arrays.stream(counts).filter(c -> c > 0).count();
        if (nonZero <= 1) {
            return 0.0d; // 单类分布零多样性
        }
        return entropyBits(counts) / (Math.log(nonZero) / Math.log(BASE_2));
    }
}
