package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会话扰乱预算装配属性（spec 318 / T628，前缀
 * {@code buzhou.session.disruption-budget}，K8s PodDisruptionBudget 借鉴）。
 * 未配置 = 不装配（零变化）；配置即要求会话索引在场（计数源）。
 *
 * @param minAvailable 保底可用 ACTIVE 会话数（≥0；0 = 不限；排水额度 = ACTIVE − 本值）
 */
@ConfigurationProperties(prefix = "buzhou.session.disruption-budget")
public record SessionDisruptionBudgetProperties(Long minAvailable) {

    public SessionDisruptionBudgetProperties {
        if (minAvailable != null && minAvailable < 0) {
            throw new BuzhouConfigurationException(
                    "buzhou.session.disruption-budget.min-available（" + minAvailable + "）非法",
                    ">= 0（0 = 不限；未配置 = 不装配预算）");
        }
    }
}
