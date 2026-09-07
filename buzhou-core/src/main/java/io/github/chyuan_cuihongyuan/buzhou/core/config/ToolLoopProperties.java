package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工具循环断路器装配属性（spec 327 / T645，前缀
 * {@code buzhou.runaway.tool-loop}）。未配置 window = 不装配（零变化）；
 * 装配即干预（默认拦——循环工具在烧真配额）。
 *
 * @param window 同工具同参数连续多少次开始拦（≥2）
 */
@ConfigurationProperties(prefix = "buzhou.runaway.tool-loop")
public record ToolLoopProperties(Integer window) {

    public ToolLoopProperties {
        if (window != null && window < 2) {
            throw new BuzhouConfigurationException(
                    "buzhou.runaway.tool-loop.window（" + window + "）非法",
                    ">= 2（同 key 连续多少次开始拦；未配置 = 不装配断路器）");
        }
    }
}
