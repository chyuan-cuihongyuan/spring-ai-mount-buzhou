package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 在线采样 yml 面（spec 407 / T706，Honeycomb 借鉴）：
 * {@code buzhou.eval.sampling.{enabled, dataset, rate-percent, min-input-chars}}。
 * 声明即挂 hook + 暴露 EvalDatasetStore bean（集必须预建——采样不建集）。
 */
@ConfigurationProperties(prefix = "buzhou.eval.sampling")
public record BuzhouEvalSamplingProperties(Boolean enabled, String dataset,
        Integer ratePercent, Integer minInputChars) {

    public BuzhouEvalSamplingProperties {
        ratePercent = ratePercent == null ? 0 : ratePercent;
        minInputChars = minInputChars == null ? 0 : minInputChars;
    }
}
