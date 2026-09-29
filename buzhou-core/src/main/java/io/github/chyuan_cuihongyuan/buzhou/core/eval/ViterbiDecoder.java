package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;

/**
 * 维特比解码（spec 8033 / V8067 / impl 2335）——
 * Viterbi 1967 思想（CDMA/GSM 同源）：**按时刻保每个状态
 * 的最大路径概率 + 回溯指针还原最可能隐状态序列**——
 * 全路径枚举 O(S^T)（时长放大）的病解。表非随机归一（行和
 * =1）校验、维度不符、空观测 fail-fast；确定性纯函数
 * （同表同观测同序列）。
 *
 * <p>与 PowerIteration（spec 8032）同族不同面：序列隐状态
 * vs 谱主分量。
 */
public final class ViterbiDecoder {

    private ViterbiDecoder() {
    }

    /**
     * 最可能隐状态序列。
     *
     * @param initial    初始概率（长 S）
     * @param transitions 转移矩阵（S×S，行和=1）
     * @param emissions   发射矩阵（T×S，每行和=1）
     */
    public static int[] decode(double[] initial, double[][] transitions, double[][] emissions) {
        if (initial == null || initial.length == 0) {
            throw new IllegalArgumentException("初始概率非空");
        }
        if (transitions == null || transitions.length != initial.length) {
            throw new IllegalArgumentException("转移表 S×S（实际 "
                    + (transitions == null ? -1 : transitions.length) + " vs " + initial.length + "）");
        }
        int states = initial.length;
        for (double[] row : transitions) {
            if (row == null || row.length != states) {
                throw new IllegalArgumentException("转移表行宽=states");
            }
            if (!normalized(row)) {
                throw new IllegalArgumentException("转移行归一（行和 " + sum(row) + "）");
            }
        }
        if (!normalized(initial)) {
            throw new IllegalArgumentException("初始概率归一（和 " + sum(initial) + "）");
        }
        if (emissions == null || emissions.length == 0) {
            throw new IllegalArgumentException("观测非空");
        }
        for (double[] row : emissions) {
            if (row == null || row.length != states) {
                throw new IllegalArgumentException("发射表行宽=states");
            }
            if (!normalized(row)) {
                throw new IllegalArgumentException("发射行归一（行和 " + sum(row) + "）");
            }
        }
        int time = emissions.length;
        double[][] scores = new double[time][states];
        int[][] backpointer = new int[time][states];
        for (int s = 0; s < states; s++) {
            scores[0][s] = initial[s] * emissions[0][s];
            backpointer[0][s] = -1;
        }
        for (int t = 1; t < time; t++) {
            for (int s = 0; s < states; s++) {
                double best = -1;
                int bestPrevious = 0;
                for (int previous = 0; previous < states; previous++) {
                    double candidate = scores[t - 1][previous] * transitions[previous][s];
                    if (candidate > best) {
                        best = candidate;
                        bestPrevious = previous;
                    }
                }
                scores[t][s] = best * emissions[t][s];
                backpointer[t][s] = bestPrevious;
            }
        }
        int[] path = new int[time];
        int last = 0;
        for (int s = 1; s < states; s++) {
            if (scores[time - 1][s] > scores[time - 1][last]) {
                last = s;
            }
        }
        path[time - 1] = last;
        for (int t = time - 2; t >= 0; t--) {
            path[t] = backpointer[t + 1][path[t + 1]];
        }
        return path;
    }

    private static boolean normalized(double[] distribution) {
        return Math.abs(sum(distribution) - 1) < 1e-9;
    }

    private static double sum(double[] distribution) {
        return Arrays.stream(distribution).sum();
    }
}
