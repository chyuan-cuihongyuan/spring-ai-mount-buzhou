package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * HMM 前向后向（spec 10018 / X10037 / impl 2421）——Rabiner 1989
 * 思想（「缩放 α/β 递推」——htk/hts 语音同源）：**α_t(i)=P(o₁..t,
 * q_t=i) 前向、β_t(i)=P(o_{t+1}..T|q_t=i) 后向，逐步缩放系数 c_t
 * 防下溢，ln 似然=∑ln c_t**——Viterbi（已占）最优路径的「全路径
 * 似然」镜像面（BaumWelch 期望统计的基座）。维数/观测越域/负概
 * 率/零概率序列 fail-fast；长序列缩放下溢护栏；确定性。
 */
public final class ForwardBackward {

    private ForwardBackward() {
    }

    /**
     * 运行结果（alpha/beta 为缩放后值；logLikelihood=ln P(O|λ)）。
     */
    public record Result(double[][] alpha, double[][] beta, double logLikelihood) {
    }

    /**
     * 前向后向联合递推。
     *
     * @param a 状态转移 N×N
     * @param b 发射 N×M
     * @param pi 初始分布 N
     * @param obs 观测索引序列（值域 [0,M)）
     * @throws IllegalArgumentException 维数/越域/负值/零概率序列
     */
    public static Result run(double[][] a, double[][] b, double[] pi, int[] obs) {
        validate(a, b, pi, obs);
        int n = pi.length;
        int t = obs.length;
        double[][] alpha = new double[t][n];
        double[] scales = new double[t];
        double[] raw = new double[n];
        for (int i = 0; i < n; i++) {
            raw[i] = pi[i] * b[i][obs[0]];
        }
        scales[0] = sum(raw);
        requirePositive(scales[0], 0);
        for (int i = 0; i < n; i++) {
            alpha[0][i] = raw[i] / scales[0];
        }
        double logLik = Math.log(scales[0]);
        for (int step = 1; step < t; step++) {
            for (int j = 0; j < n; j++) {
                double acc = 0.0;
                for (int i = 0; i < n; i++) {
                    acc += alpha[step - 1][i] * a[i][j];
                }
                raw[j] = acc * b[j][obs[step]];
            }
            scales[step] = sum(raw);
            requirePositive(scales[step], step);
            for (int j = 0; j < n; j++) {
                alpha[step][j] = raw[j] / scales[step];
            }
            logLik += Math.log(scales[step]);
        }
        double[][] beta = new double[t][n];
        for (int i = 0; i < n; i++) {
            beta[t - 1][i] = 1.0;
        }
        for (int step = t - 2; step >= 0; step--) {
            for (int i = 0; i < n; i++) {
                double acc = 0.0;
                for (int j = 0; j < n; j++) {
                    acc += a[i][j] * b[j][obs[step + 1]] * beta[step + 1][j];
                }
                beta[step][i] = acc / scales[step + 1];
            }
        }
        return new Result(alpha, beta, logLik);
    }

    private static void requirePositive(double scale, int step) {
        if (scale <= 0.0) {
            throw new IllegalArgumentException("观测序列概率为正（步 " + step + " 缩放 " + scale + "）");
        }
    }

    private static double sum(double[] v) {
        double s = 0.0;
        for (double x : v) {
            s += x;
        }
        return s;
    }

    private static void validate(double[][] a, double[][] b, double[] pi, int[] obs) {
        if (pi == null || pi.length == 0) {
            throw new IllegalArgumentException("初始分布非空非 null");
        }
        if (obs == null || obs.length == 0) {
            throw new IllegalArgumentException("观测序列非空非 null");
        }
        int n = pi.length;
        if (a == null || a.length != n) {
            throw new IllegalArgumentException("转移矩阵 N×N 相配");
        }
        if (b == null || b.length != n) {
            throw new IllegalArgumentException("发射矩阵 N×M 相配");
        }
        int m = b[0] == null ? -1 : b[0].length;
        for (double[] row : a) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("转移矩阵行宽相配");
            }
            for (double v : row) {
                if (v < 0.0) {
                    throw new IllegalArgumentException("概率非负（转移含 " + v + "）");
                }
            }
        }
        for (double[] row : b) {
            if (row == null || row.length != m) {
                throw new IllegalArgumentException("发射矩阵行宽相配");
            }
            for (double v : row) {
                if (v < 0.0) {
                    throw new IllegalArgumentException("概率非负（发射含 " + v + "）");
                }
            }
        }
        for (int o : obs) {
            if (o < 0 || o >= m) {
                throw new IllegalArgumentException("观测索引域 [0," + m + ")（实际 " + o + "）");
            }
        }
    }
}
