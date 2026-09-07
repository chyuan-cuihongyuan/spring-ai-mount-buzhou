package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 错误预算装配属性（spec 321 / T634，前缀 {@code buzhou.error-budget}，
 * Google SRE 错误预算借鉴）。未配置 slo = 不装配（零变化）；null 字段落
 * {@link ErrorBudget.Config} 默认。
 *
 * @param slo               SLO 百分比（0 &lt; slo &lt; 100，如 99.9）
 * @param window            燃尽观察窗（默认 10m；须 ≥ buckets 毫秒）
 * @param buckets           桶环桶数（默认 60）
 * @param burnRateThreshold 燃尽告警阈值（默认 2.0——两倍速烧预算）
 * @param minSamples        判 breach 最小窗内样本（默认 20，防「一败 100%」噪声）
 */
@ConfigurationProperties(prefix = "buzhou.error-budget")
public record ErrorBudgetProperties(Double slo, Duration window, Integer buckets,
        Double burnRateThreshold, Integer minSamples) {

    public ErrorBudgetProperties {
        if (slo != null && (slo <= 0 || slo >= 100)) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.slo（" + slo + "）非法",
                    "∈ (0,100)（百分比，如 99.9；未配置 = 不装配错误预算）");
        }
        if (burnRateThreshold != null && burnRateThreshold <= 0) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.burn-rate-threshold（" + burnRateThreshold + "）非法",
                    "> 0（burn 1 = 按计划烧，默认 " + ErrorBudget.Config.DEFAULT_BURN_THRESHOLD + "）");
        }
        if (buckets != null && buckets < 2) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.buckets（" + buckets + "）非法",
                    ">= 2（默认 " + ErrorBudget.Config.DEFAULT_BUCKETS + "）");
        }
        if (window != null && (window.isZero() || window.isNegative())) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.window（" + window + "）非法",
                    "正时长（默认 " + ErrorBudget.Config.DEFAULT_WINDOW + "）");
        }
        if (minSamples != null && minSamples < 1) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.min-samples（" + minSamples + "）非法",
                    ">= 1（默认 " + ErrorBudget.Config.DEFAULT_MIN_SAMPLES + "）");
        }
    }

    /** 落默认成 {@link ErrorBudget.Config}（slo 必须已配置）。 */
    public ErrorBudget.Config toConfig() {
        if (slo == null) {
            throw new BuzhouConfigurationException(
                    "buzhou.error-budget.slo 未配置", "配置后才能构造错误预算");
        }
        return new ErrorBudget.Config(slo,
                burnRateThreshold == null ? ErrorBudget.Config.DEFAULT_BURN_THRESHOLD : burnRateThreshold,
                buckets == null ? ErrorBudget.Config.DEFAULT_BUCKETS : buckets,
                window == null ? ErrorBudget.Config.DEFAULT_WINDOW : window,
                minSamples == null ? ErrorBudget.Config.DEFAULT_MIN_SAMPLES : minSamples);
    }
}
