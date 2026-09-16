package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

/**
 * 记忆强度三分量评分（spec 2003 / T3107 / impl 1554）——mem0 思想：
 * 记忆分 = 加权(recency, frequency, importance)——检索排序与衰减淘汰的
 * 统一排序键。recency = 2^(−Δt/halfLife) 半衰期衰减（Δt=半衰期时恰
 * 0.5）、frequency = 1−1/(1+ln(1+count)) 对数饱和（重访增益边际递减）、
 * importance = [0,1] 钳制直通；加权和按权和归一，输出恒 [0,1]。
 *
 * <p>纯函数零状态、确定性；参数畸形 fail-fast。
 */
public final class MemoryStrengthScore {

    /** 默认半衰期（毫秒）——24h 后 recency 分量衰减至 0.5。 */
    public static final long DEFAULT_HALF_LIFE_MILLIS = 24L * 60 * 60 * 1000;

    /** 默认权重（mem0 论文口径近似：近因 0.5 / 频度 0.3 / 重要度 0.2）。 */
    public static final Weights DEFAULT_WEIGHTS = new Weights(0.5d, 0.3d, 0.2d);

    /** 三分量权重：各 ≥ 0 且不全零（fail-fast）；构造后不可变。 */
    public record Weights(double recency, double frequency, double importance) {
        public Weights {
            if (!(recency >= 0) || Double.isNaN(recency)
                    || !(frequency >= 0) || Double.isNaN(frequency)
                    || !(importance >= 0) || Double.isNaN(importance)) {
                throw new IllegalArgumentException("权重须非负非 NaN");
            }
            if (recency + frequency + importance <= 0) {
                throw new IllegalArgumentException("权重不能全零");
            }
        }
    }

    private MemoryStrengthScore() {
    }

    /**
     * 三分量加权评分 ∈ [0,1]。契约：millisSinceLastAccess ≥ 0、
     * accessCount ≥ 0（fail-fast）；importance 钳制 [0,1]；halfLife &gt; 0。
     */
    public static double score(long millisSinceLastAccess, long accessCount,
                               double importance, Weights weights, long halfLifeMillis) {
        if (millisSinceLastAccess < 0) {
            throw new IllegalArgumentException("millisSinceLastAccess 须 ≥ 0：" + millisSinceLastAccess);
        }
        if (accessCount < 0) {
            throw new IllegalArgumentException("accessCount 须 ≥ 0：" + accessCount);
        }
        if (!(halfLifeMillis > 0)) {
            throw new IllegalArgumentException("halfLifeMillis 须 > 0：" + halfLifeMillis);
        }
        Weights w = weights == null ? DEFAULT_WEIGHTS : weights;
        double recency = Math.pow(2.0d, -(double) millisSinceLastAccess / halfLifeMillis);
        double frequency = 1.0d - 1.0d / (1.0d + Math.log1p(accessCount));
        double clampedImportance = Math.max(0.0d, Math.min(1.0d, importance));
        double weightSum = w.recency() + w.frequency() + w.importance();
        return (w.recency() * recency + w.frequency() * frequency
                + w.importance() * clampedImportance) / weightSum;
    }

    /** 默认半衰期便捷口径。 */
    public static double score(long millisSinceLastAccess, long accessCount,
                               double importance, Weights weights) {
        return score(millisSinceLastAccess, accessCount, importance, weights,
                DEFAULT_HALF_LIFE_MILLIS);
    }
}
