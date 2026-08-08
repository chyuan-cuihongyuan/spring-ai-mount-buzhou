package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * OTel 导出桥入口（spec 03「OTel 导出桥」）。
 *
 * <p>产出 {@link PipelineSink}，由装配侧经
 * {@code ObservabilityModule.configure(..., List.of(bridge.sink()))} 接入采集管线。
 * 缺省关闭：{@link OtelBridgeConfig#enabled()} 为 {@code false} 时，装配侧不应创建本桥（不接 sink 即零开销）。
 *
 * <p>三种装配形态：
 * <ul>
 *   <li>{@link #forTracer} —— 调用方自管 OTel SDK（如 Spring Boot 的 OTel autoconfig 已提供 {@link Tracer}），
 *       本桥仅做映射，不持有 SDK 生命周期；</li>
 *   <li>{@link #withExporter} —— 自建 {@link SdkTracerProvider}（{@link SimpleSpanProcessor}，逐 span 同步导出，
 *       适合测试 / 小规模），{@link #close()} 时 flush + shutdown；</li>
 *   <li>{@link #otlp} —— 自建 OTLP HTTP/protobuf 导出器（endpoint 复用 {@code otel.exporter.otlp.endpoint}，
 *       默认 {@code http://localhost:4318/v1/traces}）+ {@link BatchSpanProcessor} 异步批量导出。</li>
 * </ul>
 */
public final class OtelBridge implements AutoCloseable {

    /** 默认 OTLP traces 端点（OTel 标准约定，可被 {@code otel.exporter.otlp.endpoint} 覆盖）。 */
    public static final String DEFAULT_OTLP_ENDPOINT = "http://localhost:4318/v1/traces";

    private final PipelineSink sink;
    private final SdkTracerProvider ownedProvider;

    private OtelBridge(PipelineSink sink, SdkTracerProvider ownedProvider) {
        this.sink = sink;
        this.ownedProvider = ownedProvider;
    }

    /** 调用方自管 SDK：仅传入 {@link Tracer}，本桥不做生命周期管理。 */
    public static OtelBridge forTracer(Tracer tracer, OtelBridgeConfig config) {
        return new OtelBridge(new OtelBridgeSink(tracer, config), null);
    }

    /** 自建 SDK：{@link SimpleSpanProcessor} 逐 span 同步导出到给定 {@link SpanExporter}（测试用 {@code InMemorySpanExporter}）。 */
    public static OtelBridge withExporter(SpanExporter exporter, OtelBridgeConfig config) {
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        Tracer tracer = provider.get(OtelBridge.class.getName());
        return new OtelBridge(new OtelBridgeSink(tracer, config), provider);
    }

    /** 自建 SDK + OTLP HTTP 导出器 + {@link BatchSpanProcessor}（生产形态）。 */
    public static OtelBridge otlp(String endpoint, OtelBridgeConfig config) {
        String resolved = (endpoint == null || endpoint.isBlank()) ? DEFAULT_OTLP_ENDPOINT : endpoint;
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder().setEndpoint(resolved).build();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();
        Tracer tracer = provider.get(OtelBridge.class.getName());
        return new OtelBridge(new OtelBridgeSink(tracer, config), provider);
    }

    /** 接入采集管线的 sink。 */
    public PipelineSink sink() {
        return sink;
    }

    /** flush + 关闭自建的 SDK（仅 {@link #withExporter}/{@link #otlp} 形态有效）。 */
    @Override
    public void close() {
        if (ownedProvider != null) {
            ownedProvider.close();
        }
    }
}
