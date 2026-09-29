package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * CUSUM 变点检测（spec 8028 / V8057 / impl 2330）——
 * Page 1954 思想（SPC 质量控制同源）：**双单边累计偏移
 * C+=max(0,C+x−drift)、C−=max(0,C−x−drift)，越阈值 h 即
 * 三态判定**——小漂移在累计里可见（固定阈值把自然波动当
 * 告警、小漂移累积不可见的双病根）。越界即复位重监（信号
 * 面明示——不复位则一越永响）；drift≥0/h>0 越域 fail-fast；
 * 确定性纯状态机。
 *
 * <p>与 EwmaEstimator（spec 2027）同族不同面：平滑读数 vs
 * 变点判定。
 */
public final class CusumDetector {

    /** feed 三态：正常。 */
    public static final int NORMAL = 0;
    /** feed 三态：正漂越界。 */
    public static final int POSITIVE_SHIFT = 1;
    /** feed 三态：负漂越界。 */
    public static final int NEGATIVE_SHIFT = -1;

    private final double drift;
    private final double threshold;
    private double positiveSum;
    private double negativeSum;
    private long feeds;

    /** 构建（drift≥0/h>0 越域 fail-fast）。 */
    public CusumDetector(double drift, double threshold) {
        if (drift < 0) {
            throw new IllegalArgumentException("漂移容限非负（实际 " + drift + "）");
        }
        if (threshold <= 0) {
            throw new IllegalArgumentException("阈值为正（实际 " + threshold + "）");
        }
        this.drift = drift;
        this.threshold = threshold;
    }

    /** 喂入一个观测（返回三态；越界侧累计复位重监——明示）。 */
    public int feed(double x) {
        feeds++;
        positiveSum = Math.max(0, positiveSum + x - drift);
        negativeSum = Math.max(0, negativeSum - x - drift);
        if (positiveSum >= threshold) {
            positiveSum = 0;
            return POSITIVE_SHIFT;
        }
        if (negativeSum >= threshold) {
            negativeSum = 0;
            return NEGATIVE_SHIFT;
        }
        return NORMAL;
    }

    /** 复位累计（参数不变）。 */
    public void reset() {
        positiveSum = 0;
        negativeSum = 0;
        feeds = 0;
    }

    /** 已喂观测数（审计面）。 */
    public long feeds() {
        return feeds;
    }
}
