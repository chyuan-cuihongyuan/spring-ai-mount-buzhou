package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1436 / T2174：事件时序单调性审计——单调序列零逆序、回拨逆序显形
 * （对数/最大倒退/首逆序位）、等时刻不算逆序、空输入哨兵。
 */
class EventOrderAuditTest {

    private static EventRecord event(String id, Instant at) {
        return new EventRecord(id, "span", "s-ord", "THINKING", at, Map.of());
    }

    @Test
    void emptyInputYieldsZeroReport() {
        var r = EventOrderAudit.analyze(List.of());
        assertThat(r.totalEvents()).isZero();
        assertThat(r.inversions()).isZero();
        assertThat(r.firstInversionIndex()).isEqualTo(-1);
    }

    @Test
    void monotonicSequenceHasZeroInversions() {
        var r = EventOrderAudit.analyze(List.of(
                event("e1", Instant.parse("2026-09-14T10:00:00Z")),
                event("e2", Instant.parse("2026-09-14T10:00:01Z")),
                event("e3", Instant.parse("2026-09-14T10:00:02Z"))));
        assertThat(r.inversions()).isZero();
        assertThat(r.maxInversionMillis()).isZero();
        assertThat(r.firstInversionIndex()).isEqualTo(-1);
    }

    @Test
    void equalTimestampsAreNotInversions() {
        var r = EventOrderAudit.analyze(List.of(
                event("e1", Instant.parse("2026-09-14T10:00:00Z")),
                event("e2", Instant.parse("2026-09-14T10:00:00Z"))));
        assertThat(r.inversions()).isZero();
    }

    @Test
    void clockBackdateExposesInversions() {
        var r = EventOrderAudit.analyze(List.of(
                event("e1", Instant.parse("2026-09-14T10:00:00Z")),
                event("e2", Instant.parse("2026-09-14T10:00:05Z")),
                event("e3", Instant.parse("2026-09-14T09:59:57Z")), // 倒退 8 秒
                event("e4", Instant.parse("2026-09-14T10:00:01Z")))); // 又倒退 4 秒? no: 09:59:57→10:00:01 前进
        assertThat(r.totalEvents()).isEqualTo(4);
        assertThat(r.inversions()).isEqualTo(1);
        assertThat(r.maxInversionMillis()).isEqualTo(8000);
        assertThat(r.firstInversionIndex()).isEqualTo(2);
    }

    @Test
    void nullTimestampsSkippedNotCrash() {
        var r = EventOrderAudit.analyze(List.of(
                event("e1", Instant.parse("2026-09-14T10:00:00Z")),
                new EventRecord("e2", "span", "s-ord", "X", null, Map.of()),
                event("e3", Instant.parse("2026-09-14T10:00:01Z"))));
        assertThat(r.inversions()).isZero();
        assertThat(r.totalEvents()).isEqualTo(3);
    }
}
