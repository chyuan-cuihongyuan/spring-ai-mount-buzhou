package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

/**
 * Thompson 采样（spec 8024 / V8049 / impl 2326）——
 * Thompson 1933 贝叶斯 bandit 思想：**每臂按其后验
 * Beta(α=1+成功, β=1+失败) 抽一笔，取样本最大者**——探索
 * 随不确定性收缩（不确定臂样本散、常被抽中；信息足臂收敛、
 * 只在真优时被选）——固定贪心锁死次优臂的病解。种子化
 * Random 注入（同种子同选择序列完全确定）；null/空臂/负
 * 计数 fail-fast。Beta 采样经 Marsaglia-Tsang Gamma（α<1
 * boost 变换）。
 *
 * <p>与 AliasMethod（spec 8025）同族不同面：后验探索 vs
 * 权重精确采样。
 */
public final class ThompsonSampler {

    private ThompsonSampler() {
    }

    /** 按各臂后验采样选臂（并列取最小下标；null/空臂/负计数 fail-fast）。 */
    public static int select(long[] successes, long[] failures, Random random) {
        if (successes == null || failures == null) {
            throw new IllegalArgumentException("臂计数表非空引用");
        }
        if (successes.length == 0 || successes.length != failures.length) {
            throw new IllegalArgumentException("双表同长非空（实际 "
                    + successes.length + "/" + failures.length + "）");
        }
        for (int arm = 0; arm < successes.length; arm++) {
            if (successes[arm] < 0 || failures[arm] < 0) {
                throw new IllegalArgumentException("计数非负（臂 " + arm + "）");
            }
        }
        int best = 0;
        double bestSample = Double.NEGATIVE_INFINITY;
        for (int arm = 0; arm < successes.length; arm++) {
            double alpha = 1 + successes[arm];
            double beta = 1 + failures[arm];
            double sample = betaSample(alpha, beta, random);
            if (sample > bestSample) {
                bestSample = sample;
                best = arm;
            }
        }
        return best;
    }

    /** Beta(α,β) 采样 = X/(X+Y)，X~Gamma(α,1)，Y~Gamma(β,1)。 */
    private static double betaSample(double alpha, double beta, Random random) {
        double x = gammaSample(alpha, random);
        double y = gammaSample(beta, random);
        return x / (x + y);
    }

    /** Marsaglia-Tsang Gamma 采样（α<1 经 boost 变换）。 */
    private static double gammaSample(double shape, Random random) {
        if (shape < 1) {
            double boost = random.nextDouble();
            return gammaSample(shape + 1, random) * Math.pow(boost, 1.0 / shape);
        }
        double d = shape - 1.0 / 3;
        double c = 1.0 / Math.sqrt(9 * d);
        while (true) {
            double x = normalSample(random);
            double v = 1 + c * x;
            if (v <= 0) {
                continue;
            }
            v = v * v * v;
            double u = random.nextDouble();
            if (u < 1 - 0.0331 * x * x * x * x) {
                return d * v;
            }
            if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) {
                return d * v;
            }
        }
    }

    private static double normalSample(Random random) {
        double u1 = Math.max(random.nextDouble(), Double.MIN_VALUE);
        double u2 = random.nextDouble();
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }
}
