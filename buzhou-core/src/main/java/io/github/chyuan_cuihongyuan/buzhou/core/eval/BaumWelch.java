package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * Baum–Welch EM 重估（spec 10019 / X10039 / impl 2422）——Baum 1970
 * 思想（「期望步+极大步交替」——语音/生信 HMM 训练同源）：**E 步
 * 前向后向算 γ/ξ 期望计数，M 步按极大似然闭式重估 A/B/π，似然
 * 单调不减（EM 保证）**——ForwardBackward（10018）的消费面。
 * 迭代上限/维数 fail-fast；重估行随机归一；似然可观测面；确定性。
 */
public final class BaumWelch {

    private BaumWelch() {
    }

    /**
     * 重估结果（行随机归一后的 A/B/π 与最终 ln 似然）。
     */
    public record Estimate(double[][] a, double[][] b, double[] pi, double logLikelihood) {
    }

    /**
     * EM 重估（iterations 轮 E+M）。
     *
     * @throws IllegalArgumentException 迭代上限非法/模型契约违反
     */
    public static Estimate reestimate(double[][] a, double[][] b, double[] pi,
                                      int[] obs, int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("迭代上限为正（实际 " + iterations + "）");
        }
        double[][] currentA = a;
        double[][] currentB = b;
        double[] currentPi = pi;
        double logLik = Double.NEGATIVE_INFINITY;
        for (int iter = 0; iter < iterations; iter++) {
            ForwardBackward.Result fb = ForwardBackward.run(currentA, currentB, currentPi, obs);
            logLik = fb.logLikelihood();
            int n = currentPi.length;
            int t = obs.length;
            int m = currentB[0].length;
            double[][] gamma = new double[t][n];
            for (int step = 0; step < t; step++) {
                double denom = 0.0;
                for (int i = 0; i < n; i++) {
                    gamma[step][i] = fb.alpha()[step][i] * fb.beta()[step][i];
                    denom += gamma[step][i];
                }
                for (int i = 0; i < n; i++) {
                    gamma[step][i] /= denom;
                }
            }
            double[][] newA = new double[n][n];
            double[][] newB = new double[n][m];
            double[] newPi = gamma[0].clone();
            for (int step = 0; step < t - 1; step++) {
                double denom = 0.0;
                double[][] xi = new double[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        xi[i][j] = fb.alpha()[step][i] * currentA[i][j]
                                * currentB[j][obs[step + 1]] * fb.beta()[step + 1][j];
                        denom += xi[i][j];
                    }
                }
                requirePositive(denom, step);
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        newA[i][j] += xi[i][j] / denom;
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                double rowSum = 0.0;
                for (int j = 0; j < n; j++) {
                    rowSum += newA[i][j];
                }
                for (int j = 0; j < n; j++) {
                    newA[i][j] = rowSum > 0 ? newA[i][j] / rowSum : 1.0 / n;
                }
            }
            for (int i = 0; i < n; i++) {
                for (int k = 0; k < m; k++) {
                    double num = 0.0;
                    double denom = 0.0;
                    for (int step = 0; step < t; step++) {
                        denom += gamma[step][i];
                        if (obs[step] == k) {
                            num += gamma[step][i];
                        }
                    }
                    newB[i][k] = denom > 0 ? num / denom : 1.0 / m;
                }
            }
            currentA = newA;
            currentB = newB;
            currentPi = newPi;
        }
        ForwardBackward.Result finalFb = ForwardBackward.run(currentA, currentB, currentPi, obs);
        return new Estimate(currentA, currentB, currentPi, finalFb.logLikelihood());
    }

    private static void requirePositive(double denom, int step) {
        if (denom <= 0.0) {
            throw new IllegalArgumentException("期望计数为正（步 " + step + " 分母 " + denom + "）");
        }
    }
}
