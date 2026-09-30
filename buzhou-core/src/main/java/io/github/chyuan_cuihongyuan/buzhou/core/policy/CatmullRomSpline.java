package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * Catmull–Rom 样条（spec 10015 / X10031 / impl 2418）——Catmull–Rom
 * 1974 思想（「过控制点的张量插值」——THREE.js/游戏引擎相机轨迹
 * 同源）：**均匀参数化张量 0.5 张力——每段由四控制点三次插值，
 * 过全部控制点且 C¹ 连续**——路径平滑/相机轨迹的标准插值样条
 * （贝塞尔不过控制点的病解互补面——BezierCurve（9034）已占异面）。
 * 端点虚拟重影点（首尾复制）；参数 t ∈ [0, n−1] 越域 fail-fast；
 * null/点数 <2/非二维 fail-fast；同输入同曲线确定。
 */
public final class CatmullRomSpline {

    /** 张量张力系数（标准均匀 Catmull–Rom——0.5）。 */
    private static final double TENSION_HALF = 0.5;

    private final double[][] points;

    /**
     * 构造（控制点表，n≥2；端点虚拟重影内部处理）。
     *
     * @throws IllegalArgumentException null/点数 <2/非二维坐标
     */
    public CatmullRomSpline(double[][] controlPoints) {
        if (controlPoints == null || controlPoints.length < 2) {
            throw new IllegalArgumentException("控制点数≥2 且非 null（实际 "
                    + (controlPoints == null ? "null" : controlPoints.length) + "）");
        }
        for (double[] p : controlPoints) {
            if (p == null || p.length != 2) {
                throw new IllegalArgumentException("控制点须二维坐标");
            }
        }
        this.points = new double[controlPoints.length][];
        for (int i = 0; i < controlPoints.length; i++) {
            this.points[i] = controlPoints[i].clone();
        }
    }

    /**
     * 求值（t∈[0, n−1]——整数落控制点，小数段内插值）。
     *
     * @throws IllegalArgumentException 参数越域
     */
    public double[] point(double t) {
        int last = points.length - 1;
        if (!(t >= 0.0 && t <= last) || !Double.isFinite(t)) {
            throw new IllegalArgumentException("参数域 [0," + last + "]（实际 " + t + "）");
        }
        int segment = Math.min((int) t, last - 1);
        double s = t - segment;
        double[] p0 = points[Math.max(segment - 1, 0)];
        double[] p1 = points[segment];
        double[] p2 = points[segment + 1];
        double[] p3 = points[Math.min(segment + 2, last)];
        return new double[]{
                evaluate(p0[0], p1[0], p2[0], p3[0], s),
                evaluate(p0[1], p1[1], p2[1], p3[1], s)
        };
    }

    /** 控制点个数。 */
    public int size() {
        return points.length;
    }

    /** 单坐标三次张量插值（0.5 张力均匀 Catmull–Rom 基）。 */
    private static double evaluate(double p0, double p1, double p2, double p3, double s) {
        double s2 = s * s;
        double s3 = s2 * s;
        return TENSION_HALF * ((2.0 * p1)
                + (-p0 + p2) * s
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * s2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * s3);
    }
}
