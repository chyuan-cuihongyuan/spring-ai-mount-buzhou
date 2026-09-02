package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 重复检测装配属性（spec 326 / T643，前缀
 * {@code buzhou.runaway.repetition}，context rot 检测）。未配置 window =
 * 不装配（零变化）。
 *
 * @param window            连续相似多少条 fire（≥2；未配 = 不装配）
 * @param similarityPercent 相似阈值百分比（(0,100]，默认 80）
 * @param unstick           fire 当次 block 回填解困指令（默认 false——
 *                          observe-only；true 是行为变化宿主显式开）
 */
@ConfigurationProperties(prefix = "buzhou.runaway.repetition")
public record RepetitionProperties(Integer window, Double similarityPercent,
        Boolean unstick) {

    public RepetitionProperties {
        if (window != null && window < 2) {
            throw new BuzhouConfigurationException(
                    "buzhou.runaway.repetition.window（" + window + "）非法",
                    ">= 2（连续相似多少条判定打转；未配置 = 不装配检测）");
        }
        if (similarityPercent != null && (similarityPercent <= 0 || similarityPercent > 100)) {
            throw new BuzhouConfigurationException(
                    "buzhou.runaway.repetition.similarity-percent（" + similarityPercent + "）非法",
                    "∈ (0,100]（词元 Jaccard 相似阈值，默认 80）");
        }
    }
}
