package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * impl-655 / spec 902：pass@k 无偏估计（OpenAI HumanEval / Codex 论文 §2.1——
 * 生成类任务「k 次采样至少一次通过的概率」的正确口径）。
 *
 * <p>单项无偏估计：{@code pass@k = 1 − C(n−c, k) / C(n, k)}，实现取等价连乘形式
 * {@code 1 − ∏_{i=0}^{k−1} (n−c−i)/(n−i)}——无阶乘溢出、数值稳定。多项聚合按
 * 论文口径逐项估计后算术平均（每项须同为 n 次采样才公平）。
 *
 * <p>纯函数不触 store（EvalFlakinessDetector 同型纪律）；k 次采样由宿主多次
 * {@link EvalRunner#run} 后自行喂入（spec 513「k 次留宿主循环」边界一致）。
 */
public final class EvalPassAtK {

    private EvalPassAtK() {
    }

    /**
     * 单项无偏 pass@k：n 次采样中 c 次通过、取 k 次的至少一次通过概率。
     *
     * @param n 采样总次数（≥ 1）
     * @param c 通过次数（0 ≤ c ≤ n）
     * @param k 每次抽取的样本数（1 ≤ k ≤ n）
     */
    public static double estimate(int n, int c, int k) {
        validate(n, c, k);
        double probAllFail = 1.0;
        for (int i = 0; i < k; i++) {
            probAllFail *= (double) (n - c - i) / (n - i);
        }
        return 1.0 - probAllFail;
    }

    /**
     * 多项聚合：每项 n 次采样（通过次数 {@code passCounts[i]}）逐项估计后算术平均。
     *
     * @param passCounts 每项通过次数（0 ≤ passCounts[i] ≤ n；空数组约定 0.0）
     * @param n          每项采样总次数（所有项同 n）
     * @param k          每次抽取的样本数
     */
    public static double aggregate(int[] passCounts, int n, int k) {
        if (passCounts == null || passCounts.length == 0) {
            return 0.0;
        }
        return Arrays.stream(passCounts).asDoubleStream()
                .map(c -> estimate(n, (int) c, k))
                .average()
                .orElse(0.0);
    }

    private static void validate(int n, int c, int k) {
        if (n < 1) {
            throw new IllegalArgumentException("n 须 ≥ 1（采样次数），收到 " + n);
        }
        if (c < 0 || c > n) {
            throw new IllegalArgumentException("c 须 ∈ [0, n]，收到 " + c + "（n=" + n + "）");
        }
        if (k < 1 || k > n) {
            throw new IllegalArgumentException("k 须 ∈ [1, n]，收到 " + k + "（n=" + n + "）");
        }
    }
}
