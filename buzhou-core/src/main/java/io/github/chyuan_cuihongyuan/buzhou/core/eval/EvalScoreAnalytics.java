package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
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

    /**
     * impl-656 / spec 903：bootstrap 均值区间（percentile 口径）。{@code lower ≤
     * pointEstimate ≤ upper} 不由构造保证（重采样经验分布偏斜时点估计可落区间外——
     * 诚实呈现）；调用方按需自行判断。
     */
    public record MeanInterval(double lower, double upper, double pointEstimate) {
    }

    private EvalScoreAnalytics() {
    }

    /**
     * spec 747 / T1092 族（731 深化，反事实阈值对照）：给定候选阈值集，分别
     * 计算「若以该阈值为判定线会有多少分数通过」——调阈值的放行量影响一目
     * 了然。分数来源同 {@link #similarityScores}（detail 解析口径）。
     *
     * @return 阈值 → 通过数（阈值按入参顺序；null 入参 fail-fast）
     */
    public static Map<Double, Integer> passesAtThresholds(EvalRunResult run, double... thresholds) {
        if (run == null) {
            throw new IllegalArgumentException("run 必须非空");
        }
        List<Double> scores = similarityScores(run).scores();
        Map<Double, Integer> result = new LinkedHashMap<>();
        for (double threshold : thresholds) {
            int passes = 0;
            for (double score : scores) {
                if (score >= threshold) {
                    passes++;
                }
            }
            result.put(threshold, passes);
        }
        return result;
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

    /**
     * impl-656 / spec 903：bootstrap 均值置信区间（Efron percentile 口径）——
     * 小样本 mean 的抽样误差显形（零分布假设：评估分数常见 pass/fail 双峰，
     * 不满足正态近似）。
     *
     * <p>可重复重采样 {@code resamples} 次（每次含 {@code samples.length} 个
     * 有放回抽取）→ 每次算均值 → 经验分布 {@code [α/2, 1−α/2]} 分位点为区间。
     * {@code seed} 显式注入：同 seed 同结果（可复现，与依赖注入时钟 spec 9
     * 同风格）；单样本退化为点估计区间（重采样恒同值——诚实语义）。
     *
     * @param samples         样本分数（非空；NaN 元素 fail-fast——均值语义已被污染）
     * @param confidenceLevel 置信水平 ∈ 开区间 (0,1)
     * @param resamples       重采样次数（≥ 1）
     * @param seed            随机种子（同 seed 同结果）
     */
    public static MeanInterval bootstrapMeanInterval(double[] samples, double confidenceLevel,
                                                     int resamples, long seed) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("samples 必须非空");
        }
        for (double s : samples) {
            if (Double.isNaN(s)) {
                throw new IllegalArgumentException("samples 含 NaN——均值语义已被污染，先清洗");
            }
        }
        if (confidenceLevel <= 0 || confidenceLevel >= 1) {
            throw new IllegalArgumentException("confidenceLevel 须 ∈ 开区间 (0,1)，收到 " + confidenceLevel);
        }
        if (resamples < 1) {
            throw new IllegalArgumentException("resamples 须 ≥ 1，收到 " + resamples);
        }
        java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
        int n = samples.length;
        double pointEstimate = Arrays.stream(samples).average().orElse(Double.NaN);
        double[] resampleMeans = new double[resamples];
        for (int r = 0; r < resamples; r++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += samples[rnd.nextInt(n)];
            }
            resampleMeans[r] = sum / n;
        }
        Arrays.sort(resampleMeans);
        double alpha = (1.0 - confidenceLevel) / 2.0;
        int lowerIdx = (int) Math.floor(alpha * resamples);
        int upperIdx = (int) Math.ceil((1.0 - alpha) * resamples) - 1;
        // 索引夹取（极端 α×resamples < 1 时退化到经验分布两端——诚实语义）
        lowerIdx = Math.max(0, Math.min(resamples - 1, lowerIdx));
        upperIdx = Math.max(0, Math.min(resamples - 1, upperIdx));
        return new MeanInterval(resampleMeans[lowerIdx], resampleMeans[upperIdx], pointEstimate);
    }
}
