package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 错误偏向采样 yml 面（spec 423 / T738，OTel tail_sampling「ERROR 全保」
 * 借鉴）：{@code buzhou.eval.error-sampling.{enabled, dataset,
 * error-rate-percent, min-input-chars}}。声明即装配 per-session
 * {@code TurnErrorSampler} 观察者（错误轮入候选池——成功路仍归
 * buzhou.eval.sampling）；error-rate-percent 默认 100。
 */
@ConfigurationProperties(prefix = "buzhou.eval.error-sampling")
public record BuzhouErrorSamplingProperties(Boolean enabled, String dataset,
        Integer errorRatePercent, Integer minInputChars) {

    public BuzhouErrorSamplingProperties {
        errorRatePercent = errorRatePercent == null ? 100 : errorRatePercent;
        minInputChars = minInputChars == null ? 0 : minInputChars;
    }
}
