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

    private static String preview(String s) {
        return s.length() > 64 ? "\"" + s.substring(0, 64) + "…\"" : "\"" + s + "\"";
    }
}
