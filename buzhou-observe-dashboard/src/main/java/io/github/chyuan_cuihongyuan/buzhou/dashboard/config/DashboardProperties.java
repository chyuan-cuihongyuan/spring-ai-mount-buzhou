package io.github.chyuan_cuihongyuan.buzhou.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可视化后台装配属性（spec 03 / 09 / ticket 22，前缀 {@code buzhou.observe.dashboard}）。
 *
 * @param enabled    总开关（默认 false，开发调试时开启）
 * @param port       独立端口；{@code 0} = 随机端口（默认）
 * @param pathPrefix 静态资源与 API 前缀（默认 {@code /buzhou}）
 */
@ConfigurationProperties(prefix = "buzhou.observe.dashboard")
public record DashboardProperties(Boolean enabled, Integer port, String pathPrefix) {

    public DashboardProperties {
        enabled = enabled == null ? false : enabled;
        port = port == null ? 0 : port;
        pathPrefix = (pathPrefix == null || pathPrefix.isBlank()) ? "/buzhou" : pathPrefix;
    }
}
