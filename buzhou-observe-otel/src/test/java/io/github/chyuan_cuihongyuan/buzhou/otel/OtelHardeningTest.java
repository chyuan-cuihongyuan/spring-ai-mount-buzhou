package io.github.chyuan_cuihongyuan.buzhou.otel;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.otel.config.BuzhouOtelAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.otel.config.OtelProperties;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-47 / spec 14 §C 加固面测试：未终态 span 有界驱逐、tracer 模式缺 bean 启动失败、
 * exporter-mode 枚举 fail-fast、timeout 校验、otlp 缺省装配。
 */
class OtelHardeningTest {

    /** 未终态 span 超上限：被驱逐（end + buzhou.evicted 属性），护栏生效。 */
    @Test
    void overBudgetOpenSpansAreEvictedWithMarker() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OtelBridgeSink sink = new OtelBridgeSink(
                provider.get(OtelBridge.class.getName()), OtelBridgeConfig.enabledDefaults(), 3, 100);

        // 塞 5 条 RUNNING（上限 3），不回终态：驱逐护栏必须触发
        for (int i = 0; i < 5; i++) {
            sink.onSpan(runningSpan("span-" + i));
        }
        var finished = exporter.getFinishedSpanItems();
        assertThat(finished).anyMatch(s -> Boolean.TRUE.equals(s.getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.booleanKey("buzhou.evicted"))));
        assertThat(sink.evictedSpans()).isGreaterThanOrEqualTo(2);
        provider.close();
    }

    /** tracer 模式缺 Tracer bean：启动失败（此前静默回退 otlp，显式意图被忽略）。 */
    @Test
    void tracerModeWithoutBeanFailsFast() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "buzhou.observe.otel.enabled=true",
                        "buzhou.observe.otel.exporter-mode=tracer")
                .withUserConfiguration(BuzhouOtelAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BuzhouConfigurationException.class);
                    assertThat(String.valueOf(context.getStartupFailure()))
                            .contains("exporter-mode=tracer")
                            .contains("Tracer");
                });
    }

    /** exporter-mode 拼错：属性绑定即失败（带修法）。 */
    @Test
    void invalidExporterModeFailsFast() {
        assertThatThrownBy(() -> new OtelProperties(true, false, "trace", null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("exporter-mode")
                .hasMessageContaining("otlp")
                .hasMessageContaining("tracer");
    }

    /** timeout 非正：绑定即失败。 */
    @Test
    void invalidTimeoutFailsFast() {
        assertThatThrownBy(() -> new OtelProperties(true, false, "otlp", null, null,
                java.time.Duration.ZERO))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("timeout");
    }

    /** otlp 模式缺省参数可装配（headers 空 + 默认超时）：bridge bean 正常创建。 */
    @Test
    void otlpModeWithDefaultsWires() {
        new ApplicationContextRunner()
                .withPropertyValues("buzhou.observe.otel.enabled=true")
                .withUserConfiguration(BuzhouOtelAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OtelBridge.class);
                    assertThat(context).hasBean("otelPipelineSink");
                });
    }

    // ---- helpers ----

    private static SpanRecord runningSpan(String spanId) {
        return new SpanRecord(spanId, null, "sess-otel", 1, "MODEL_CALL", "model-call",
                Instant.now(), null, "RUNNING", Map.of("model.name", "test"));
    }
}
