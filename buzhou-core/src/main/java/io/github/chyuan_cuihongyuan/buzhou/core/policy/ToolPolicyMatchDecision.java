package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Objects;

/**
 * impl-753 / spec 1000：单次工具策略匹配决策（OPA decision log 借鉴——
 * 「谁、按哪条规则、判成什么」一读即知，不只有最终生效策略）。
 *
 * @param toolName   被匹配的工具名（非空）
 * @param outcome    决策分类：{@code EXACT} 精确名命中 / {@code GLOB} 通配模式命中 /
 *                   {@code NONE} 未命中（返回空 Map）
 * @param matchedKey EXACT→工具名本身；GLOB→胜出的通配模式（最长前缀）；NONE→空串
 */
public record ToolPolicyMatchDecision(String toolName, Outcome outcome, String matchedKey) {

    /** 决策分类。 */
    public enum Outcome {
        /** 精确名命中。 */
        EXACT,
        /** 通配模式命中（多模式命中时最长前缀胜出）。 */
        GLOB,
        /** 未命中（返回空 Map）。 */
        NONE
    }

    public ToolPolicyMatchDecision {
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(outcome, "outcome");
        matchedKey = matchedKey == null ? "" : matchedKey;
    }
}
