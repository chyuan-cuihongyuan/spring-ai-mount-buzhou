package io.github.chyuan_cuihongyuan.buzhou.otel.config;

import io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTel 导出桥装配属性（spec 03 / 09 / ticket 22，前缀 {@code buzhou.observe.otel}）。
 *
 * <p>总开关 {@code enabled} 默认关（spec 09：引入模块仍需显式开启才导出）；本属性类的 {@code enabled}
 * 与 AutoConfig 的 {@code @ConditionalOnProperty} 同源。命名对齐 spec 09 配置表（用 {@code observe.otel}）。
 *
 * @param enabled        总开关（默认 false）
 * @param includeContent 导出属性是否携带思维链/回复正文/工具入参出参原文（默认 false）
 * @param exporterMode   导出形态：{@code tracer}（调用方自管 SDK，需 Tracer bean）或 {@code otlp}（默认，自建 OTLP）
 * @param endpoint       OTLP traces 端点（默认 {@link OtelBridge#DEFAULT_OTLP_ENDPOINT}）
 */
@ConfigurationProperties(prefix = "buzhou.observe.otel")
public record OtelProperties(Boolean enabled, Boolean includeContent, String exporterMode, String endpoint) {

    public OtelProperties {
        enabled = enabled == null ? false : enabled;
        includeContent = includeContent == null ? false : includeContent;
        exporterMode = (exporterMode == null || exporterMode.isBlank()) ? "otlp" : exporterMode;
        endpoint = (endpoint == null || endpoint.isBlank()) ? OtelBridge.DEFAULT_OTLP_ENDPOINT : endpoint;
    }
}
