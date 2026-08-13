package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.ToolPolicyMatcher;

import java.util.Map;

/**
 * 溢出阈值/durable 判定的单一事实源（T20 token-aware + T22 durable 覆盖）：
 * {@code SpillOffloadHook}（即时溢出）与 {@code HotTailViewProcessor}（视图级溢出）共用，
 * 避免两处策略解析漂移。
 */
final class SpillThresholds {

    private SpillThresholds() {
    }

    /** token→字符换算系数（与 core CharHeuristicTokenEstimator 的 4 字符/token 启发式一致）。 */
    static final int CHARS_PER_TOKEN_ESTIMATE = 4;

    /**
     * 解析工具阈值：per-tool {@code spillThresholdTokens}（优先，×4 折算字符）
     * → per-tool {@code spillThresholdChars} → 全局默认。
     */
    static int thresholdFor(Map<String, Object> toolPolicies, String toolName, int defaultThresholdChars) {
        Map<String, Object> policy = ToolPolicyMatcher.match(toolPolicies, toolName);
        int charsFromTokens = parsePositive(policy.get("spillThresholdTokens")) * CHARS_PER_TOKEN_ESTIMATE;
        if (charsFromTokens > 0) {
            return charsFromTokens;
        }
        int chars = parsePositive(policy.get("spillThresholdChars"));
        return chars > 0 ? chars : defaultThresholdChars;
    }

    /** T22 durable：声明「永不溢出」的工具输出保持全量内联。 */
    static boolean isDurable(Map<String, Object> toolPolicies, String toolName) {
        Object value = ToolPolicyMatcher.match(toolPolicies, toolName).get("spillNeverOffload");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int parsePositive(Object value) {
        if (value instanceof Number number) {
            return number.intValue() > 0 ? number.intValue() : 0;
        }
        if (value instanceof String text) {
            try {
                int parsed = Integer.parseInt(text.trim());
                return parsed > 0 ? parsed : 0;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
