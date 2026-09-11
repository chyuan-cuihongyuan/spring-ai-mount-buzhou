package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 在线实验分桶 yml 面（spec 505 / T762，GrowthBook/Statsig 思想）：
 * {@code buzhou.experiments.<experiment>.<variant> = 权重（整数百分比）}。
 * 权重和 ≤100、余量 = 未入组；map 非空才装配（Binder 预绑——409 同法）。
 */
@ConfigurationProperties(prefix = "buzhou.experiments")
public record BuzhouExperimentProperties(Map<String, Map<String, Integer>> experiments) {

    public BuzhouExperimentProperties {
        experiments = experiments == null ? Map.of() : Map.copyOf(experiments);
    }
}
