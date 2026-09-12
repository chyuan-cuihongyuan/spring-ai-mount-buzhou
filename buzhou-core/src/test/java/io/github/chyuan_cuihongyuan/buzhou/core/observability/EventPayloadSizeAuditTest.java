package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 732 / T1064–T1065：事件 payload 大小审计——类型聚合降序、字节口径、
 * 序列化失败跳过、null fail-fast。
 */
class EventPayloadSizeAuditTest {

    private static EventRecord event(String type, Map<String, Object> payload) {
        return new EventRecord("e", "s", "sess", type,
                Instant.parse("2026-09-13T00:00:00Z"), payload);
    }

    @Test
    void aggregatesBytesByTypeDescending() {
        EventPayloadSizeAudit.Report report = EventPayloadSizeAudit.analyze(List.of(
                event("big", Map.of("text", "x".repeat(100))),
                event("big", Map.of("text", "y".repeat(50))),
                event("small", Map.of("ok", 1))));
        assertThat(report.rows()).extracting(EventPayloadSizeAudit.Row::type)
                .containsExactly("big", "small");
        assertThat(report.rows().get(0).count()).isEqualTo(2);
        assertThat(report.rows().get(0).maxBytes())
                .isGreaterThan(report.rows().get(0).totalBytes() / 2);
        assertThat(report.totalBytes())
                .isEqualTo(report.rows().stream().mapToLong(EventPayloadSizeAudit.Row::totalBytes).sum());
        assertThat(report.serialized()).isEqualTo(3);
        assertThat(report.skipped()).isZero();
    }

    @Test
    void emptyAndNull() {
        assertThat(EventPayloadSizeAudit.analyze(List.of()).rows()).isEmpty();
        assertThatThrownBy(() -> EventPayloadSizeAudit.analyze(null))
                .isInstanceOf(NullPointerException.class);
    }
}
