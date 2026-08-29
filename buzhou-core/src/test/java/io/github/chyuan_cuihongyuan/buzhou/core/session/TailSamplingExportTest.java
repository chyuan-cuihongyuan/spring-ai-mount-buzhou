package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 136 §B / T461：尾采样导出红队——错误会话（ERROR span）与慢会话（span
 * 超阈值）100% 保留；健康快会话按确定性哈希比率留样（同 id 重导同判定——
 * at-least-once 一致）；rate=0 只留错误+慢；空结果诚实零行；策略参数
 * fail-fast。借鉴：OpenTelemetry tail sampling。
 */
class TailSamplingExportTest {

    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");

    private final InMemoryObservabilityStore store = new InMemoryObservabilityStore();
    private final ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(store);

    private static SpanRecord span(String id, String session, String status,
                                   Duration duration) {
        return new SpanRecord(id, null, session, 1, "MODEL", "chat", T0,
                T0.plus(duration), status, Map.of());
    }

    private void seedThreeSessions() {
        store.saveSpans(List.of(
                span("e1", "sess-err", SpanStatus.ERROR, Duration.ofSeconds(1)),
                span("s1", "sess-slow", SpanStatus.OK, Duration.ofSeconds(5)),
                span("h1", "sess-healthy", SpanStatus.OK, Duration.ofMillis(200))));
        store.saveEvents(List.of(
                new EventRecord("ev-1", "e1", "sess-err", "tool-error",
                        T0.plusMillis(10), Map.of()),
                new EventRecord("ev-2", "h1", "sess-healthy", "thinking",
                        T0.plusMillis(20), Map.of())));
    }

    @Test
    void errorAndSlowSessionsAlwaysKeptRateZeroDropsHealthy() throws Exception {
        seedThreeSessions();
        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.SampledExportResult result = exporter.exportAllSampled(
                out, ObservabilityJsonlExporter.TailSamplingPolicy.of(0, Duration.ofSeconds(1)));

        assertThat(result.keptSessions()).isEqualTo(2);
        assertThat(result.notSampledSessions()).isEqualTo(1);
        assertThat(result.spans()).isEqualTo(2);
        assertThat(result.events()).isEqualTo(1); // 只有 sess-err 的事件
        assertThat(result.summary())
                .isEqualTo("tail-sampled export: kept=2/3, spans=2, events=1, degraded=0");
        assertThat(out.toString()).contains("sess-err").contains("sess-slow");
        assertThat(out.toString()).doesNotContain("sess-healthy");
    }

    @Test
    void deterministicBySessionIdAcrossRuns() throws Exception {
        store.saveSpans(List.of(span("h1", "sess-healthy", SpanStatus.OK,
                Duration.ofMillis(100))));
        ObservabilityJsonlExporter.TailSamplingPolicy policy =
                ObservabilityJsonlExporter.TailSamplingPolicy.of(50, Duration.ofSeconds(1));
        boolean expectedIn = ("sess-healthy".hashCode() & Integer.MAX_VALUE) % 100 < 50;

        StringWriter first = new StringWriter();
        StringWriter second = new StringWriter();
        ObservabilityJsonlExporter.SampledExportResult r1 = exporter.exportAllSampled(first, policy);
        ObservabilityJsonlExporter.SampledExportResult r2 = exporter.exportAllSampled(second, policy);

        assertThat(r1.keptSessions()).isEqualTo(expectedIn ? 1 : 0);
        assertThat(r2.keptSessions()).isEqualTo(r1.keptSessions()); // 同 id 同判定
        assertThat(first.toString()).isEqualTo(second.toString());
    }

    @Test
    void emptyStoreExportsZeroLinesHonest() throws Exception {
        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.SampledExportResult result = exporter.exportAllSampled(
                out, ObservabilityJsonlExporter.TailSamplingPolicy.of(100, Duration.ofSeconds(1)));
        assertThat(result.keptSessions()).isZero();
        assertThat(result.notSampledSessions()).isZero();
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void fullRateKeepsEverything() throws Exception {
        seedThreeSessions();
        StringWriter out = new StringWriter();
        ObservabilityJsonlExporter.SampledExportResult result = exporter.exportAllSampled(
                out, ObservabilityJsonlExporter.TailSamplingPolicy.of(100, Duration.ofSeconds(1)));
        assertThat(result.keptSessions()).isEqualTo(3);
        assertThat(result.notSampledSessions()).isZero();
        assertThat(out.toString()).contains("sess-healthy");
    }

    @Test
    void policyValidatedFailFast() throws Exception {
        assertThatThrownBy(() -> ObservabilityJsonlExporter.TailSamplingPolicy
                .of(101, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObservabilityJsonlExporter.TailSamplingPolicy
                .of(-1, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ObservabilityJsonlExporter.TailSamplingPolicy
                .of(10, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exporter.exportAllSampled(new StringWriter(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
