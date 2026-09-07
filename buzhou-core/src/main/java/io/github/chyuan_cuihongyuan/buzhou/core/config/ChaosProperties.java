package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 工具混沌注入装配属性（spec 322 / T635，前缀 {@code buzhou.chaos}，
 * Netflix Chaos Monkey 借鉴）。默认关（enabled 未配/false 不装配）；
 * 两档袭击概率独立，0 = 该档不袭。
 *
 * @param enabled          总开关（false = 不装配）
 * @param latencyPercent   延迟袭击概率百分比 [0,100]（默认 0）
 * @param latencyMillis    延迟毫秒（默认 0——0 则延迟档不生效）
 * @param exceptionPercent 故障袭击概率百分比 [0,100]（默认 0）
 * @param tools            include 清单（空/未配 = 全量工具）
 */
@ConfigurationProperties(prefix = "buzhou.chaos")
public record ChaosProperties(Boolean enabled, Double latencyPercent, Long latencyMillis,
        Double exceptionPercent, List<String> tools) {

    public ChaosProperties {
        if (latencyPercent != null && (latencyPercent < 0 || latencyPercent > 100)) {
            throw new BuzhouConfigurationException(
                    "buzhou.chaos.latency-percent（" + latencyPercent + "）非法",
                    "∈ [0,100]（百分比；0 = 延迟档不袭）");
        }
        if (exceptionPercent != null && (exceptionPercent < 0 || exceptionPercent > 100)) {
            throw new BuzhouConfigurationException(
                    "buzhou.chaos.exception-percent（" + exceptionPercent + "）非法",
                    "∈ [0,100]（百分比；0 = 故障档不袭）");
        }
        if (latencyMillis != null && latencyMillis < 0) {
            throw new BuzhouConfigurationException(
                    "buzhou.chaos.latency-millis（" + latencyMillis + "）非法",
                    ">= 0（0 = 延迟档不生效）");
        }
    }
}
