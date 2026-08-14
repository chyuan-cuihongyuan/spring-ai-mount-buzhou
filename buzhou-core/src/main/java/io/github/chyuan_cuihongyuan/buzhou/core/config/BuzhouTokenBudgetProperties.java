package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Token/成本预算装配属性（spec 16，前缀 {@code buzhou.token-budget}，T83 / impl-58）。
 *
 * <p>会话级 token/成本累计（SessionStateStore 持久化）+ 三硬顶预算闸。字段 boxed，null = 不限
 * （对齐 {@code BuzhouRunawayProperties} 模板，safe-by-default：不设阈值 = 空转零开销）。
 *
 * @param enabled                机制总开关（默认开；关则完全旁路，等价现状）
 * @param maxSessionPromptTokens 会话累计输入 token 硬顶（null = 不限）
 * @param maxSessionTotalTokens  会话累计总 token（prompt+completion）硬顶（null = 不限）
 * @param maxSessionCostUsd      会话累计成本硬顶（USD；null = 不限；<b>必须配 pricing，否则启动失败</b>）
 * @param pricing                价目表：model → 每百万 token 单价（USD）；无该模型条目 = 零成本
 */
@ConfigurationProperties(prefix = "buzhou.token-budget")
public record BuzhouTokenBudgetProperties(
        Boolean enabled,
        Long maxSessionPromptTokens,
        Long maxSessionTotalTokens,
        BigDecimal maxSessionCostUsd,
        Map<String, Pricing> pricing) {

    public BuzhouTokenBudgetProperties {
        enabled = enabled == null || enabled;
        if (maxSessionPromptTokens != null && maxSessionPromptTokens < 1) {
            throw configError("max-session-prompt-tokens", "设为正整数，或删除该键（不限）");
        }
        if (maxSessionTotalTokens != null && maxSessionTotalTokens < 1) {
            throw configError("max-session-total-tokens", "设为正整数，或删除该键（不限）");
        }
        if (maxSessionCostUsd != null && maxSessionCostUsd.signum() <= 0) {
            throw configError("max-session-cost-usd", "设为正数（USD），或删除该键（不限）");
        }
        if (maxSessionCostUsd != null && (pricing == null || pricing.isEmpty())) {
            throw configError("max-session-cost-usd",
                    "设了成本上限但没有 buzhou.token-budget.pricing.* 价目——成本闸无从计算；"
                            + "请补 pricing.<model>.input-per-million / output-per-million，或删除成本上限");
        }
        if (pricing != null) {
            pricing.forEach((model, p) -> {
                if (p == null || p.inputPerMillion() == null || p.outputPerMillion() == null
                        || p.inputPerMillion().signum() < 0 || p.outputPerMillion().signum() < 0) {
                    throw configError("pricing." + model + ".input/output-per-million",
                            "两个单价都设为 >= 0 的数（USD / 每百万 token）");
                }
            });
        }
    }

    /** 全默认（装配测试用；全部 null = 不限，等价现状）。 */
    public static BuzhouTokenBudgetProperties defaults() {
        return new BuzhouTokenBudgetProperties(null, null, null, null, null);
    }

    /** 是否配置了任何硬顶（都没有 = 计量仍累计，闸门空转）。 */
    public boolean anyCapConfigured() {
        return maxSessionPromptTokens != null || maxSessionTotalTokens != null || maxSessionCostUsd != null;
    }

    /**
     * 每百万 token 单价（USD）。整数 microUsd 口径：token × 每百万价 = microUsd/token。
     */
    public record Pricing(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
    }

    private static BuzhouConfigurationException configError(String key, String action) {
        return new BuzhouConfigurationException(
                "buzhou.token-budget." + key + " 非法", action);
    }
}
