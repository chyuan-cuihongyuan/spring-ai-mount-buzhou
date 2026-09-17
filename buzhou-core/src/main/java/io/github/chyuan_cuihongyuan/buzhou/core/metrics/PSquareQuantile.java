package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * P² 流式分位数（spec 3006 / T5013 / impl 2007）——Jain-Chlamtac
 * P² 算法思想：**五标记点增量估计**——min / p/2 / p / (1+p)/2 / max
 * 五个标记随流推进，中标记（p 位）即估计值；抛物线预测（三点拟合）
 * 为主、越界退线性插值，位置每步 ±1 逼近理想位。**O(1) 空间**——
 * 免存全样本（精确分位需 O(n) 空间），大样本光滑分布误差 O(1/n)。
 *
 * <p>前 5 样本缓冲排序初始化（count&lt;5 诚实口径：排序缓冲按下标
 * 取值）；count=0 estimate 为 NaN；p∈(0,1) 严格开区间（min/max 是
 * 另一个更简单的问题）。非线程安全（单流口径）。
 */
public final class PSquareQuantile {

    /** P² 标记点数（min、p/2、p、(1+p)/2、max——论文定死）。 */
    private static final int MARKER_COUNT = 5;

    /** 初始化所需样本数（等于标记点数）。 */
    private static final int INIT_SAMPLE_COUNT = 5;

    /** 最后一个内部标记下标（调整循环 1..3，min/max 直接赋值）。 */
    private static final int LAST_INTERIOR_MARKER = 3;

    private final double p;
    private final double[] initBuffer = new double[INIT_SAMPLE_COUNT];
    private long count;
    private final double[] heights = new double[MARKER_COUNT];
    private final long[] positions = new long[MARKER_COUNT];
    private final double[] desired = new double[MARKER_COUNT];

    /** 目标分位 p∈(0,1)（0.5=中位数）。 */
    public PSquareQuantile(double p) {
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("p∈(0,1) 开区间：" + p);
        }
        this.p = p;
    }

    /** 目标分位（读回）。 */
    public double quantile() {
        return p;
    }

    /** 已吸收样本数。 */
    public long count() {
        return count;
    }

    /**
     * 增量吸收一个样本：定位 cell（标记区间）→ 位置账面递增 →
     * 内部标记 1..3 逐步逼近理想位（抛物线优先，越界退线性）。
     */
    public void add(double x) {
        count++;
        if (count <= INIT_SAMPLE_COUNT) {
            initBuffer[(int) (count - 1)] = x;
            if (count == INIT_SAMPLE_COUNT) {
                initializeMarkers();
            }
            return;
        }
        int cell = locateCell(x);
        for (int i = cell + 1; i <= LAST_INTERIOR_MARKER; i++) {
            positions[i]++;
        }
        positions[LAST_INTERIOR_MARKER + 1] = count;
        refreshDesiredPositions();
        for (int i = 1; i <= LAST_INTERIOR_MARKER; i++) {
            adjustMarker(i);
        }
    }

    /**
     * 当前分位估计：count&lt;5 排序缓冲下标取值（诚实口径）；
     * 初始化后即中标记（p 位）高度。
     */
    public double estimate() {
        if (count == 0) {
            return Double.NaN;
        }
        if (count < INIT_SAMPLE_COUNT) {
            double[] sorted = Arrays.copyOf(initBuffer, (int) count);
            Arrays.sort(sorted);
            int idx = (int) Math.min(Math.floor(p * (count - 1)), count - 1);
            return sorted[idx];
        }
        return heights[2];
    }

    private void initializeMarkers() {
        double[] sorted = initBuffer.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < MARKER_COUNT; i++) {
            heights[i] = sorted[i];
            positions[i] = i + 1;
        }
    }

    /** 样本落点：q[cell] ≤ x &lt; q[cell+1]；越界两端直接改写端标记。 */
    private int locateCell(double x) {
        if (x < heights[0]) {
            heights[0] = x;
            return 0;
        }
        if (x >= heights[MARKER_COUNT - 1]) {
            heights[MARKER_COUNT - 1] = x;
            return MARKER_COUNT - 1;
        }
        int cell = 0;
        while (cell < LAST_INTERIOR_MARKER && x >= heights[cell + 1]) {
            cell++;
        }
        return cell;
    }

    /** 理想位：n'[i] = 1 + (count−1)·p_i（首尾恒 1 与 count——自证）。 */
    private void refreshDesiredPositions() {
        double[] markerQuantiles = {0, p / 2, p, (1 + p) / 2, 1};
        for (int i = 0; i < MARKER_COUNT; i++) {
            desired[i] = 1 + (count - 1) * markerQuantiles[i];
        }
    }

    /** 单标记一步逼近：d≥1 且右邻距 >1 / d≤−1 且左邻距 >1 才动。 */
    private void adjustMarker(int i) {
        double d = desired[i] - positions[i];
        boolean canMoveUp = d >= 1 && positions[i + 1] - positions[i] > 1;
        boolean canMoveDown = d <= -1 && positions[i] - positions[i - 1] > 1;
        if (!canMoveUp && !canMoveDown) {
            return;
        }
        double step = Math.signum(d);
        double candidate = parabolicCandidate(i, step);
        if (!(heights[i - 1] < candidate && candidate < heights[i + 1])) {
            candidate = heights[i] + step * (heights[i + 1] - heights[i - 1])
                    / (positions[i + 1] - positions[i - 1]);
        }
        heights[i] = candidate;
        positions[i] += (long) step;
    }

    /** 抛物线预测（三点拟合——光滑分布主路径）。 */
    private double parabolicCandidate(int i, double d) {
        double fromLeft = (positions[i] - positions[i - 1] + d)
                * (heights[i + 1] - heights[i]) / (positions[i + 1] - positions[i]);
        double fromRight = (positions[i + 1] - positions[i] - d)
                * (heights[i] - heights[i - 1]) / (positions[i] - positions[i - 1]);
        return heights[i] + d / (positions[i + 1] - positions[i - 1]) * (fromLeft + fromRight);
    }
}
