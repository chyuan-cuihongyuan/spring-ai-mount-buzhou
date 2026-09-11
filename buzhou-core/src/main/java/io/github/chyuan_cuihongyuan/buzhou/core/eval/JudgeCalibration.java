package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * judge 校准跟踪（spec 516 / T783，LightEval/工业 judge calibration）：
 * judge verdict vs 金标准（断言期望）的混淆矩阵四率——judge 的宽松倾向
 * （FP 低 precision）先于版本对比被发现。判红（fail|error）为正类
 * （judge 的价值在抓坏）；单侧项排除（漂移非偏差——513 同口径）。
 * 纯函数不触 store（EvalRunDiff 同型）。
 *
 * <p>诚实边界：golden 须为断言型期望（双 judge 一致率是另一语义）；
 * 仅二值（评分制 judge 不做）；分母 0 指标 = null（诚实空值——416 同口径）。
 */
public final class JudgeCalibration {

    /** 校准报告（四率可 null=分母 0——诚实空值）。 */
    public record CalibrationReport(int tp, int tn, int fp, int fn, int singleSided,
            Double agreement, Double precision, Double recall, Double f1) {
    }

    private JudgeCalibration() {
    }

    /**
     * 校准分析：golden = 断言期望 run，judged = judge 产出 run（同 itemId
     * 对齐；单侧项排除）。
     */
    public static CalibrationReport calibrate(EvalRunResult golden, EvalRunResult judged) {
        if (golden == null || judged == null) {
            throw new IllegalArgumentException("两个 run 结果都必须非空");
        }
        Map<String, String> goldenStatus = new LinkedHashMap<>();
        golden.items().forEach(i -> goldenStatus.put(i.itemId(), i.status()));
        Map<String, String> judgedStatus = new LinkedHashMap<>();
        judged.items().forEach(i -> judgedStatus.put(i.itemId(), i.status()));

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        int singleSided = 0;
        for (String id : union(goldenStatus.keySet(), judgedStatus.keySet())) {
            String expected = goldenStatus.get(id);
            String actual = judgedStatus.get(id);
            if (expected == null || actual == null) {
                singleSided++;
                continue;
            }
            boolean goldenRed = isRed(expected);
            boolean judgeRed = isRed(actual);
            if (goldenRed && judgeRed) {
                tp++;
            } else if (!goldenRed && !judgeRed) {
                tn++;
            } else if (!goldenRed) {
                fp++; // 金标准绿、judge 判红——误报（宽松的反面：过严）
            } else {
                fn++; // 金标准红、judge 判绿——漏报（宽松倾向主症状）
            }
        }
        int n = tp + tn + fp + fn;
        Double agreement = n == 0 ? null : (double) (tp + tn) / n;
        Double precision = (tp + fp) == 0 ? null : (double) tp / (tp + fp);
        Double recall = (tp + fn) == 0 ? null : (double) tp / (tp + fn);
        Double f1 = (precision == null || recall == null || precision + recall == 0)
                ? null : 2 * precision * recall / (precision + recall);
        return new CalibrationReport(tp, tn, fp, fn, singleSided,
                agreement, precision, recall, f1);
    }

    /** 红 = fail | error（判红为正类——judge 价值在抓坏）。 */
    private static boolean isRed(String status) {
        return EvalRunItemResult.STATUS_FAIL.equals(status)
                || EvalRunItemResult.STATUS_ERROR.equals(status);
    }

    private static java.util.Set<String> union(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(a);
        all.addAll(b);
        return all;
    }
}
