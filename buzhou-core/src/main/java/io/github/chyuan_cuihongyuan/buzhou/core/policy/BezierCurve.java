package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * 贝塞尔曲线（spec 9034 / W9069 / impl 2387）——de Casteljau 1959
 * 思想（Bézier 1962 Renault/CAD 曲线设计同源——前端 CSS cubic-
 * bezier/SVG 路径/字体轮廓同源）：**控制点逐层线性插值
 * （t 从下到上收缩到一点）——数值稳定的几何求值**——直接展开
 * Bernstein 多项式（高阶系数大数相消）与手工拟合（无设计自由
 * 度）的中间形态。端点插值（B(0)=P₀、B(1)=Pₙ）与凸包包含
 * （曲线永不逸出控制多边形）两大不变量；pointAt 求值 +
 * flatten 折线化（等参采样）；任意维控制点；t∈[0,1] 契约
 * （越域 fail-fast）；null/单点退化（恒点）诚实边界；确定性
 * 纯函数。
 *
 * <p>与 BresenhamLine/HilbertCurve（同包）同域不同面：离散
 * 光栅 vs 连续参数曲线；与 Bezier 相关的缓动语义（exec 域
 * pacing）不同面：几何曲线 vs 时间映射。
 */
public final class BezierCurve {

    private BezierCurve() {
    }

    /**
     * 曲线点（de Casteljau 三角逐层插值）。
     *
     * @throws IllegalArgumentException null/空控制点、t 越域
     */
    public static double[] pointAt(double[][] controlPoints, double t) {
        if (controlPoints == null || controlPoints.length == 0) {
            throw new IllegalArgumentException("控制点非空（≥1 点）");
        }
        if (t < 0 || t > 1 || Double.isNaN(t)) {
            throw new IllegalArgumentException("t∈[0,1]（实际 " + t + "）");
        }
        int dimension = controlPoints[0].length;
        double[][] level = new double[controlPoints.length][];
        for (int i = 0; i < controlPoints.length; i++) {
            if (controlPoints[i] == null || controlPoints[i].length != dimension) {
                throw new IllegalArgumentException("控制点维度一致（首点 " + dimension + "）");
            }
            level[i] = controlPoints[i].clone();
        }
        for (int layer = level.length - 1; layer > 0; layer--) {
            for (int i = 0; i < layer; i++) {
                double[] merged = new double[dimension];
                for (int d = 0; d < dimension; d++) {
                    merged[d] = (1 - t) * level[i][d] + t * level[i + 1][d];
                }
                level[i] = merged;
            }
        }
        return level[0];
    }

    /**
     * 折线化（segments 段等参采样——端点含入）。
     *
     * @throws IllegalArgumentException 段数 < 1
     */
    public static List<double[]> flatten(double[][] controlPoints, int segments) {
        if (segments < 1) {
            throw new IllegalArgumentException("段数为正（实际 " + segments + "）");
        }
        List<double[]> polyline = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            polyline.add(pointAt(controlPoints, i / (double) segments));
        }
        return polyline;
    }
}
