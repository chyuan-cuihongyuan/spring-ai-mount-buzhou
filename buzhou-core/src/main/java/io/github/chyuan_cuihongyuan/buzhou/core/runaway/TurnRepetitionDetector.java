package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 轮次重复检测（spec 326 / T643，LLM 打转/context rot 检测——LangChain
 * 社区讨论库内硬化）：逐条喂数，<b>相邻</b>输出词元集 Jaccard ≥ 相似阈值
 * 计一段 run；run 达窗 fire 一次 {@link Verdict}（闩住防刷屏——不相似输出
 * 解闩重计，再打转可再 fire）。朴素词元 Jaccard：可解释可测，无模型依赖
 * （embedding 语义相似另立项）。空白/空输出视为不相似（空转不是复读）。
 */
public final class TurnRepetitionDetector {

    /** 一次打转判定（run 长 + 相似度）。 */
    public record Verdict(int runLength, double similarity) {
    }

    static final String FIRED_COUNTER = "buzhou.runaway.repetition.fired";

    private final int window;
    private final double similarityThreshold; // [0,1]

    private String previous;
    private Set<String> previousTokens;
    private int run;
    private boolean latched;

    /**
     * @param window            连续相似多少条 fire（≥2）
     * @param similarityPercent 相似阈值百分比（0,100]
     */
    public TurnRepetitionDetector(int window, double similarityPercent) {
        if (window < 2) {
            throw new IllegalArgumentException("window >= 2（当前 " + window + "）");
        }
        if (similarityPercent <= 0 || similarityPercent > 100) {
            throw new IllegalArgumentException("similarityPercent ∈ (0,100]（当前 " + similarityPercent + "）");
        }
        this.window = window;
        this.similarityThreshold = similarityPercent / 100.0;
    }

    /** 喂一条输出；达窗且未闩 → fire（本 run 只此一次）。 */
    public synchronized Optional<Verdict> record(String content) {
        Set<String> tokens = tokenize(content);
        if (tokens.isEmpty()) {
            run = 1; // 空白/空输出：空转不是复读——run 归 1 解闩
            latched = false;
            previous = null;
            previousTokens = null;
            return Optional.empty();
        }
        double similarity = 0.0;
        if (previousTokens != null) {
            similarity = jaccard(previousTokens, tokens);
        }
        if (previousTokens != null && similarity >= similarityThreshold) {
            run++;
        } else {
            run = 1; // 不相似（或首条）——新 run 开始并解闩
            latched = false;
        }
        previous = content;
        previousTokens = tokens;
        if (run >= window && !latched) {
            latched = true;
            BuzhouMetricsHolder.metrics().counter(FIRED_COUNTER);
            return Optional.of(new Verdict(run, similarity));
        }
        return Optional.empty();
    }

    /** 当前 run 长（观测面）。 */
    public synchronized int currentRun() {
        return run;
    }

    /** 手动重置（新会话窗口）。 */
    public synchronized void reset() {
        previous = null;
        previousTokens = null;
        run = 0;
        latched = false;
    }

    private static Set<String> tokenize(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(content.trim().toLowerCase().split("\\s+")));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return (double) intersection / union;
    }
}
