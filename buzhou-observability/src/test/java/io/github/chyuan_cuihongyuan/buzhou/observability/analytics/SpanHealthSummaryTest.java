package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 720 / T1040–T1041（712 修正收敛）：healthSummary——total/runningResidue/
 * errorRate 三字段、空表诚实零、null fail-fast。
 */
class SpanHealthSummaryTest {

    private static SpanRecord span(String kind, String status) {
        return new SpanRecord("s", null, "sess", 0, kind, "op",
                Instant.parse("2026-09-12T00:00:00Z"),
                "RUNNING".equals(status) ? null : Instant.parse("2026-09-12T00:01:00Z"),
                status, null);
    }

    @Test
    void healthSummaryFieldsAreExact() {
        SpanStatusDistribution.HealthSummary summary = SpanStatusDistribution.healthSummary(List.of(
                span("tool", "OK"),
                span("tool", "ERROR"),
                span("tool", "ERROR"),
                span("session", "OK"),
                span("session", "RUNNING"),
                span("llm", "CANCELLED")));
        assertThat(summary.total()).isEqualTo(6);
        assertThat(summary.runningResidue()).isEqualTo(1);
        assertThat(summary.errorRate()).isEqualTo(2.0 / 6);
    }

    @Test
    void emptyIsHonestZeroAndNullFailsFast() {
        SpanStatusDistribution.HealthSummary empty = SpanStatusDistribution.healthSummary(List.of());
        assertThat(empty.total()).isZero();
        assertThat(empty.runningResidue()).isZero();
        assertThat(empty.errorRate()).isZero();
        assertThatThrownBy(() -> SpanStatusDistribution.healthSummary(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
