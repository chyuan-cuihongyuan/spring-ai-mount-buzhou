package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 错误预算冻结装配属性（spec 335 / T662，前缀
 * {@code buzhou.backpressure.error-budget-freeze}）。enabled ≠ true = 不装配
 * （spawn 准入零变化）。
 *
 * @param enabled  开关（默认 false）
 * @param interval 评估周期（默认 15s）
 */
@ConfigurationProperties(prefix = "buzhou.backpressure.error-budget-freeze")
public record ErrorBudgetFreezeProperties(
        Boolean enabled,
        Duration interval) {

    public ErrorBudgetFreezeProperties {
        enabled = enabled == null ? Boolean.FALSE : enabled;
        interval = interval == null ? Duration.ofSeconds(15) : interval;
        if (interval.isZero() || interval.isNegative()) {
            throw new BuzhouConfigurationException(
                    "buzhou.backpressure.error-budget-freeze.interval（" + interval + "）非法",
                    "正时长，如 15s");
        }
    }
}
