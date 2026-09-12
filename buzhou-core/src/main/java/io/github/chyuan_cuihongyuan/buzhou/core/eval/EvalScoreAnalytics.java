package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评估分数分布解析（spec 731 / T1062，714 similarity 分数留痕的消费端）：
 * 从 run 明细 detail 中的 similarity=0.xxxxxx 口径解析分数做分布统计——
 * 「这批 run 的相似度都在哪」无需逐条肉眼。
 *
 * <p>纯函数：detail 无分数口径的项跳过（EXACT/CONTAINS 等——诚实计数）。
 */
public final class EvalScoreAnalytics {

    private static final Pattern SIMILARITY = Pattern.compile("similarity=([0-9]+(?:\\.[0-9]+)?)");

    /** 不可变分布报告（未解析到任何分数 → scored=0、统计 NaN）。 */
    public record Report(int scored, double min, double max, double mean, List<Double> scores) {
    }

    private EvalScoreAnalytics() {
    }

    /** 解析 run 中 similarity 口径分数并统计（null run fail-fast）。 */
    public static Report similarityScores(EvalRunResult run) {
        if (run == null) {
            throw new IllegalArgumentException("run 必须非空");
        }
        List<Double> scores = new ArrayList<>();
        for (EvalRunItemResult item : run.items()) {
            String detail = item.detail();
            if (detail == null) {
                continue;
            }
            Matcher matcher = SIMILARITY.matcher(detail);
            if (matcher.find()) {
                scores.add(Double.parseDouble(matcher.group(1)));
            }
        }
        double min = scores.isEmpty() ? Double.NaN : scores.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
        double max = scores.isEmpty() ? Double.NaN : scores.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
        double mean = scores.isEmpty() ? Double.NaN : scores.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        return new Report(scores.size(), min, max, mean, List.copyOf(scores));
    }
}
