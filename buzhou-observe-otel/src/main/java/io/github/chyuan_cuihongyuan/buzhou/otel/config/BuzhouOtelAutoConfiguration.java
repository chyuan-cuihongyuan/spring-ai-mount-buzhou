package io.github.chyuan_cuihongyuan.buzhou.otel.config;

import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridge;
import io.github.chyuan_cuihongyuan.buzhou.otel.OtelBridgeConfig;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OTel 导出桥自装配（spec 03 / 09 / ticket 22）。
 *
 * <p><b>缺省关闭</b>（{@code buzhou.observe.otel.enabled} 默认 false）：关闭时本 AutoConfig 不装配，
 * 不产出 {@link PipelineSink}，主链路零开销。显式开启后产出 sink bean，由 buzhou-observability
 * 装配经 {@code List<PipelineSink>} 收集接入采集管线（旁路导出）。
 *
 * <p>导出形态：{@code exporter-mode=tracer}（调用方自管 OTel SDK，需容器内有 {@link Tracer} bean，
 * 本桥不持有 SDK 生命周期）或 {@code otlp}（默认，自建 OTLP HTTP 导出器 + BatchSpanProcessor，
 * 销毁时 flush + shutdown）。
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@ConditionalOnProperty(prefix = "buzhou.observe.otel", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OtelProperties.class)
public class BuzhouOtelAutoConfiguration {

    @Bean(destroyMethod = "close")
    public OtelBridge otelBridge(OtelProperties props, ObjectProvider<Tracer> tracerProvider) {
        OtelBridgeConfig bridgeConfig = new OtelBridgeConfig(true, props.includeContent());
        Tracer tracer = tracerProvider.getIfAvailable();
        if ("tracer".equals(props.exporterMode())) {
            if (tracer == null) {
                // impl-47 fail-fast：显式 tracer 模式不容静默回退 otlp（此前用户意图被忽略且指向错误端点）
                throw new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException(
                        "buzhou.observe.otel.exporter-mode=tracer 但容器内无 Tracer bean",
                        "引入 OTel SDK 自动装配（如 micrometer-tracing-bridge-otel）或改用 exporter-mode=otlp");
            }
            return OtelBridge.forTracer(tracer, bridgeConfig);
        }
        return OtelBridge.otlp(props.endpoint(), bridgeConfig, props.headers(), props.timeout());
    }

    @Bean
    public PipelineSink otelPipelineSink(OtelBridge bridge) {
        return bridge.sink();
    }
}
