package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 词面漂移检测器（spec 90 §A / T343）：字符 bigram Jaccard 相似度 &lt; 阈值即漂移——
 * 零依赖默认实现（中英文通吃：CJK 逐字 bigram 天然分词，拉丁词整词归一）。
 * 阈值默认 {@value #DEFAULT_THRESHOLD}（保守：词面重叠极低才判漂移——假阴性
 * 优先于假阳性，误触发压缩的代价高于漏触发）。
 */
public final class LexicalDriftDetector implements SemanticDriftDetector {

    public static final double DEFAULT_THRESHOLD = 0.15d;

    private final double threshold;

    public LexicalDriftDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public LexicalDriftDetector(double threshold) {
        this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    }

    @Override
    public boolean drifted(String currentInput, String summaryText) {
        if (currentInput == null || currentInput.isBlank()
                || summaryText == null || summaryText.isBlank()) {
            return false; // 无基准不判漂移（诚实：缺证据不动作）
        }
        Set<String> inputGrams = bigrams(currentInput);
        Set<String> summaryGrams = bigrams(summaryText);
        inputGrams.retainAll(summaryGrams);
        double union = bigrams(currentInput).size() + summaryGrams.size() - inputGrams.size();
        double similarity = union == 0 ? 0.0 : inputGrams.size() / union;
        return similarity < threshold;
    }

    /** 归一化 bigram 集（小写；CJK 与拉丁统一处理）。 */
    private static Set<String> bigrams(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + 1 < normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        if (!normalized.isEmpty()) {
            grams.add(normalized); // 整串也入集（短文本全等场景）
        }
        return grams;
    }
}
