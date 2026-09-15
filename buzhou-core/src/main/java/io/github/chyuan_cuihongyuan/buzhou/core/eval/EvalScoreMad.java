package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 评测分数 MAD 鲁棒离散度读面（L 会话 1700 系 R1 = effort #1700 / spec 1700 /
 * 票 T2601 + T2602 / impl 1300）——Prometheus / Thanos 的鲁棒统计思想（MAD
 * 异常检测）：均值/标准差被单次离群 run 拖走，中位数/MAD 不为离群点让步。
 * 门判定（EvalGate）给瞬时过/不过，趋势（EvalPassRateTrend spec 1444）给
 * 方向，本面给离散度与离群 run 定位。
 *
 * <p>纯函数零状态：吃一批逐 run 分数，吐 median / MAD（中位数绝对偏差）/
 * 离群索引（修正 z 值 {@link #MODIFIED_Z_CONSISTENCY}·|x−median| / MAD >
 * 阈值，Iglewicz–Hoaglin 默认 {@link #DEFAULT_OUTLIER_Z}=3.5）。空/不足
 * 哨兵 {@link Dispersion#INSUFFICIENT}（mad=−1）；MAD=0 走
 * {@link Dispersion#TIGHT} 收紧档（任何偏离中位数的点直接判离群——z 值无定义）。
 *
 * @since 1.0.0
 */
public final class EvalScoreMad {

    /** 修正 z 值一致化常数（0.6745 = 1/1.4826，正态一致化因子）。 */
    public static final double MODIFIED_Z_CONSISTENCY = 0.6745;

    /** 默认离群阈值（Iglewicz–Hoaglin 推荐 3.5）。 */
    public static final double DEFAULT_OUTLIER_Z = 3.5;

    private EvalScoreMad() {
    }

    /** 离散度三档闭集。 */
    public enum Dispersion { INSUFFICIENT, TIGHT, SPREAD }

    /**
     * @param count       分数个数
     * @param median      中位数（INSUFFICIENT 时为 0）
     * @param mad         中位数绝对偏差（INSUFFICIENT 哨兵 −1；TIGHT 档为 0）
     * @param scores      入参防御性拷贝（时间序无要求）
     * @param outliers    离群索引（升序，按入参位置）
     * @param dispersion  离散度档位
     */
    public record MadReport(int count, double median, double mad, List<Double> scores,
                            List<Integer> outliers, Dispersion dispersion) {
    }

    /** 审计入口：默认阈值 3.5。 */
    public static MadReport analyze(List<Double> scores) {
        return analyze(scores, DEFAULT_OUTLIER_Z);
    }

    /** 审计入口：自定义离群阈值（修正 z 值上限）。 */
    public static MadReport analyze(List<Double> scores, double maxZ) {
        List<Double> data = List.copyOf(scores == null ? List.of() : scores);
        int n = data.size();
        if (n < 3) {
            return new MadReport(n, 0d, -1d, data, List.of(), Dispersion.INSUFFICIENT);
        }
        List<Double> sorted = new ArrayList<>(data);
        sorted.sort(Double::compare);
        double median = median(sorted);
        List<Double> absDeviations = new ArrayList<>(n);
        for (double v : data) {
            absDeviations.add(Math.abs(v - median));
        }
        absDeviations.sort(Double::compare);
        double mad = median(absDeviations);
        List<Integer> outliers = new ArrayList<>();
        Dispersion dispersion = mad > 0d ? Dispersion.SPREAD : Dispersion.TIGHT;
        for (int i = 0; i < n; i++) {
            double deviation = Math.abs(data.get(i) - median);
            boolean outlier = mad > 0d
                    ? MODIFIED_Z_CONSISTENCY * deviation / mad > maxZ
                    : deviation > 0d;
            if (outlier) {
                outliers.add(i);
            }
        }
        return new MadReport(n, median, mad, data, List.copyOf(outliers), dispersion);
    }

    private static double median(List<Double> sorted) {
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(mid)
                : (sorted.get(mid - 1) + sorted.get(mid)) / 2;
    }
}
