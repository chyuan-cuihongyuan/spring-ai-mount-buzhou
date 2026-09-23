package io.github.chyuan_cuihongyuan.buzhou.core.health;

/**
 * 复合健康分（spec 1920 / T3041 / impl 1521）——监控面板复合健康
 * 分惯例：多维健康读数（延迟/错误率/饱和度/新鲜度）按权加权合成
 * 0–100 单一分，三档判级——「总体健康吗」一句话可答，告警级 =
 * 判级。
 *
 * <p>纯函数零状态；维度采集归各健康面。
 */
public final class HealthScoreComposite {

    private HealthScoreComposite() {
    }

    /** 判级三档：HEALTHY ≥ 80 / DEGRADED ≥ 50 / UNHEALTHY（边界含下）。 */
    public enum Band { HEALTHY, DEGRADED, UNHEALTHY }

    /**
     * 加权合成：Σ(score×weight)/Σweight（0–100）。契约：scores 与
     * weights 同长且非空、score ∈ [0,100]、weight > 0（fail-fast）。
     */
    public static double composite(double[] scores, double[] weights) {
        if (scores == null || weights == null || scores.length == 0
                || scores.length != weights.length) {
            throw new IllegalArgumentException(
                    "scores/weights 须同长且非空");
        }
        double weightedSum = 0;
        double weightSum = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] < 0.0 || scores[i] > 100.0) {
                throw new IllegalArgumentException(
                        "分数须在 [0,100]：" + scores[i]);
            }
            if (weights[i] <= 0.0) {
                throw new IllegalArgumentException(
                        "权重须 > 0：" + weights[i]);
            }
            weightedSum += scores[i] * weights[i];
            weightSum += weights[i];
        }
        return weightedSum / weightSum;
    }

    /**
     * 判级：score ≥ 80 → HEALTHY；≥ 50 → DEGRADED；否则 UNHEALTHY
     * （边界含下——恰 80/恰 50 取上档）。
     */
    public static Band band(double score) {
        if (score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    "分数须在 [0,100]：" + score);
        }
        if (score >= 80.0) {
            return Band.HEALTHY;
        }
        if (score >= 50.0) {
            return Band.DEGRADED;
        }
        return Band.UNHEALTHY;
    }
}
