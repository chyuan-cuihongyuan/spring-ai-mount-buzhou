package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 指数加权移动平均估计器（spec 2027 / T3155 / impl 1578）——
 * Netflix/Finagle 指标平滑口径：estimate = α×新值 + (1−α)×旧值——
 * 观测去抖（瞬时尖峰被稀释、趋势可跟随）；α 大跟得快（惯性小），
 * α 小平滑强（惯性大）；首样本直接锚定（不从 0 爬坡）。
 *
 * <p>纯单值状态非线程安全（单写者口径——多线程由调用方外同步或
 * 每线程一件）；确定性无时钟。
 */
public final class EwmaEstimator {

    /** 默认平滑系数（α=0.2——约 5 样本记忆惯例）。 */
    public static final double DEFAULT_ALPHA = 0.2d;

    private final double alpha;
    private double estimate;
    private boolean anchored;
    private long observations;

    /** 契约：alpha ∈ (0,1]（fail-fast——0 永不更新、>1 发散）。 */
    public EwmaEstimator(double alpha) {
        if (!(alpha > 0) || alpha > 1 || Double.isNaN(alpha)) {
            throw new IllegalArgumentException("alpha 须在 (0,1]：" + alpha);
        }
        this.alpha = alpha;
    }

    public EwmaEstimator() {
        this(DEFAULT_ALPHA);
    }

    /** 观测一个值：首样本锚定，其后 α 加权递推。契约：value 有限非 NaN。 */
    public void observe(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("value 须有限非 NaN：" + value);
        }
        if (!anchored) {
            estimate = value;
            anchored = true;
        } else {
            estimate = alpha * value + (1.0d - alpha) * estimate;
        }
        observations++;
    }

    /** 当前估计（未观测 NaN——以 {@link #hasSamples()} 判）。 */
    public double estimate() {
        return anchored ? estimate : Double.NaN;
    }

    /** 是否已有观测。 */
    public boolean hasSamples() {
        return anchored;
    }

    /** 累计观测数。 */
    public long observations() {
        return observations;
    }

    /** 重置（回到未锚定态）。 */
    public void reset() {
        anchored = false;
        estimate = 0.0d;
        observations = 0;
    }
}
