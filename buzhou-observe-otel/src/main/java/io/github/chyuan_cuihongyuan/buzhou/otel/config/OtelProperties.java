package io.github.chyuan_cuihongyuan.buzhou.otel.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * OTel 导出桥装配属性（spec 03 / 09，前缀 {@code buzhou.observe.otel}；impl-47 收口）。
 *
 * <p>总开关 {@code enabled} 默认关。impl-47：<b>fail-fast</b>——{@code exporter-mode} 拼错
 * 启动即失败（此前任意字符串静默走 otlp）；OTLP headers/timeout 可配
 * （header 值支持 {@code ${ENV_VAR:}} 占位符，经 Spring 属性解析）。
 *
 * @param enabled        总开关（默认 false）
 * @param includeContent 导出属性是否携带思维链/回复正文/工具入参出参原文（默认 false）
 * @param exporterMode   导出形态：{@code tracer}（调用方自管 SDK，需 Tracer bean）或 {@code otlp}（默认，自建 OTLP）
 * @param endpoint       OTLP traces 端点（默认 {@link OtelBridge#DEFAULT_OTLP_ENDPOINT}）
 * @param headers        OTLP 请求头（如 Authorization bearer；值支持 ${ENV:} 占位）
 * @param timeout        OTLP 导出请求超时（默认 10s）
 */
@ConfigurationProperties(prefix = "buzhou.observe.otel")
public record OtelProperties(Boolean enabled, Boolean includeContent, String exporterMode, String endpoint,
                              Map<String, String> headers, Duration timeout) {

    /** 多构造器场景下显式指定绑定构造器（4 参兼容构造仅供编程式使用）。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public OtelProperties {
        enabled = enabled == null ? false : enabled;
        includeContent = includeContent == null ? false : includeContent;
        exporterMode = (exporterMode == null || exporterMode.isBlank()) ? "otlp" : exporterMode;
        endpoint = (endpoint == null || endpoint.isBlank()) ? OtelBridge.DEFAULT_OTLP_ENDPOINT : endpoint;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        // impl-47 fail-fast：exporter-mode 非法值此前静默走 otlp（显式意图被忽略）
        if (!"otlp".equals(exporterMode) && !"tracer".equals(exporterMode)) {
            throw new BuzhouConfigurationException(
                    "buzhou.observe.otel.exporter-mode=\"" + exporterMode + "\" 非法",
                    "取值只能是 otlp（默认，自建 OTLP 导出）或 tracer（复用容器 Tracer bean）");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new BuzhouConfigurationException(
                    "buzhou.observe.otel.timeout（" + timeout + "）必须为正时长", "设为正时长（默认 10s）");
        }
    }

    /** 既有 4 参构造兼容。 */
    public OtelProperties(Boolean enabled, Boolean includeContent, String exporterMode, String endpoint) {
        this(enabled, includeContent, exporterMode, endpoint, null, null);
    }
}
