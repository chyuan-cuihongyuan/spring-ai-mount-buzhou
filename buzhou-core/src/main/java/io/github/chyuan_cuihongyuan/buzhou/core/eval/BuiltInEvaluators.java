package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 内置三评估器（spec 52 §C / T192）：EXACT 全等 / CONTAINS 子串 / REGEX 正则
 * （jayway json-path 不在依赖树——勘察证实，JSON_PATH 不做，零新依赖纪律）。
 *
 * <p>REGEX 的 expected 为正则表达式，actual 全文 {@code find()} 语义（部分匹配）；
 * 非法正则构造期 fail-fast（不进 run 才炸）。
 */
public final class BuiltInEvaluators {

    private BuiltInEvaluators() {
    }

    /** EXACT：actual 与 expected 全等（trim 后比较——模型输出尾随空白容差）。 */
    public static final Evaluator EXACT = (actual, expected, item) -> {
        boolean pass = actual.trim().equals(expected.trim());
        return pass
                ? EvalScore.pass("exact 命中")
                : EvalScore.fail("expected=" + preview(expected) + " actual=" + preview(actual));
    };

    /** CONTAINS：actual 包含 expected 子串。 */
    public static final Evaluator CONTAINS = (actual, expected, item) -> {
        boolean pass = actual.contains(expected);
        return pass
                ? EvalScore.pass("contains 命中")
                : EvalScore.fail("未包含期望子串 " + preview(expected)
                        + "；actual=" + preview(actual));
    };

    /** REGEX：expected 为正则，actual 全文 find（部分匹配）；非法正则构造期 fail-fast。 */
    public static Evaluator regex(String expected) {
        final Pattern pattern;
        try {
            pattern = Pattern.compile(expected);
        } catch (PatternSyntaxException e) {
            throw new BuzhouConfigurationException(
                    "评估正则非法：" + expected + "（" + e.getDescription() + "）",
                    "修正 expected 正则后重试（JDK Pattern 语法）");
        }
        return (actual, exp, item) -> pattern.matcher(actual).find()
                ? EvalScore.pass("regex 命中")
                : EvalScore.fail("regex 未命中 " + preview(expected) + "；actual=" + preview(actual));
    }

    /**
     * SIMILARITY（spec 714 / T1028，HELM grading scales 思想）：字符 trigram
     * 集合 Jaccard 相似度 ≥ minRatio 即 pass——LLM 输出词序微变/标点差异不再
     * 脆判。语言无关（CJK/拉丁同口径）、大小写不敏感、空白折叠。detail 携带
     * 分数留痕（漂移/分布分析可解析）。minRatio ∈ [0,1] 构造 fail-fast。
     */
    public static Evaluator similarity(double minRatio) {
        if (!(minRatio >= 0.0 && minRatio <= 1.0)) {
            throw new IllegalArgumentException("similarity 阈值必须在 [0,1]（当前 " + minRatio + "）");
        }
        return (actual, expected, item) -> {
            double ratio = trigramJaccard(actual, expected);
            String detail = "similarity=" + String.format(java.util.Locale.ROOT, "%.6f", ratio)
                    + " 阈值=" + minRatio;
            return ratio >= minRatio ? EvalScore.pass(detail) : EvalScore.fail(detail);
        };
    }

    /**
     * 字符 trigram 集合 Jaccard（|A∩B|/|A∪B|）：归一化（lowercase+空白折叠）
     * 后取 3-gram；文本短于 3 字符退化为字符集合。双方全空 = 1.0（退化一致），
     * 单方空 = 0.0。
     */
    static double trigramJaccard(String actual, String expected) {
        java.util.Set<String> a = trigrams(actual);
        java.util.Set<String> b = trigrams(expected);
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String gram : a) {
            if (b.contains(gram)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return (double) intersection / union;
    }

    /** 归一化（lowercase+连续空白折叠为单空格）后取 trigram 集合；短文本退化为字符集合。 */
    private static java.util.Set<String> trigrams(String text) {
        String normalized = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
        java.util.Set<String> grams = new java.util.HashSet<>();
        if (normalized.length() < 3) {
            for (int i = 0; i < normalized.length(); i++) {
                grams.add(String.valueOf(normalized.charAt(i)));
            }
            return grams;
        }
        for (int i = 0; i <= normalized.length() - 3; i++) {
            grams.add(normalized.substring(i, i + 3));
        }
        return grams;
    }

    private static String preview(String s) {
        return s.length() > 64 ? "\"" + s.substring(0, 64) + "…\"" : "\"" + s + "\"";
    }
}
