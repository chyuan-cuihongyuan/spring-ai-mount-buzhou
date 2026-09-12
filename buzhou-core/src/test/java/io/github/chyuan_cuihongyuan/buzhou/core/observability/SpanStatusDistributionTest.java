package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 712 / T1024–T1025：span 状态分布——(kind,status) 聚合字典序、
 * RUNNING 残留、errorRate 口径、空表诚实零、null fail-fast。
 */
class SpanStatusDistributionTest {

    private static SpanRecord span(String kind, String status) {
        return new SpanRecord("s", null, "sess", 0, kind, "op",
                Instant.parse("2026-09-12T00:00:00Z"),
                SpanStatus.RUNNING.equals(status) ? null : Instant.parse("2026-09-12T00:01:00Z"),
                status, null);
    }

    @Test
    void aggregatesByKindAndStatusInDeterministicOrder() {
        SpanStatusDistribution.Report report = SpanStatusDistribution.of(List.of(
                span("tool", SpanStatus.OK),
                span("tool", SpanStatus.ERROR),
                span("tool", SpanStatus.ERROR),
                span("session", SpanStatus.OK),
                span("session", SpanStatus.RUNNING),
                span("llm", SpanStatus.CANCELLED)));
        assertThat(report.total()).isEqualTo(6);
        assertThat(report.runningResidue()).isEqualTo(1);
        assertThat(report.errorRate()).isEqualTo(2.0 / 6);
        assertThat(report.rows()).hasSize(5);
        // kind 字典序 → status 字典序
        assertThat(report.rows()).extracting(SpanStatusDistribution.Row::kind)
                .containsExactly("llm", "session", "session", "tool", "tool");
        assertThat(report.rows()).extracting(SpanStatusDistribution.Row::status)
                .containsExactly("CANCELLED", "OK", "RUNNING", "ERROR", "OK");
        assertThat(report.rows().get(3).count()).isEqualTo(2); // tool|ERROR
    }

    @Test
    void unknownStatusValuesAreAggregatedNotDropped() {
        SpanStatusDistribution.Report report = SpanStatusDistribution.of(List.of(
                span("tool", "FUTURE_STATUS")));
        assertThat(report.rows()).hasSize(1);
        assertThat(report.rows().get(0).status()).isEqualTo("FUTURE_STATUS"); // 前向兼容
        assertThat(report.total()).isEqualTo(1);
    }

    @Test
    void emptyIsHonestZeroAndNullFailsFast() {
        SpanStatusDistribution.Report empty = SpanStatusDistribution.of(List.of());
        assertThat(empty.total()).isZero();
        assertThat(empty.rows()).isEmpty();
        assertThat(empty.errorRate()).isZero();
        assertThat(empty.runningResidue()).isZero();
        assertThatThrownBy(() -> SpanStatusDistribution.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
