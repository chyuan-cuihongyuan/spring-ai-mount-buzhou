package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 735 / T1070–T1071：事件配对完整性——配对/悬空请求/孤儿应答区分、
 * HITL 规则复用、空表/null fail-fast。
 */
class EventPairingAuditTest {

    private static EventRecord event(String eventId, String spanId, String type) {
        return new EventRecord(eventId, spanId, "sess", type,
                Instant.parse("2026-09-13T00:00:00Z"), Map.of());
    }

    @Test
    void pairedAndDanglingAreDistinguished() {
        EventPairingAudit.Report report = EventPairingAudit.audit(
                List.of(
                        event("e1", "sp1", "TOOL_INPUT"),
                        event("e2", "sp1", "TOOL_OUTPUT"),
                        event("e3", "sp2", "TOOL_INPUT"),
                        event("e4", "sp3", "TOOL_OUTPUT")),
                Map.of("TOOL_INPUT", "TOOL_OUTPUT"));
        assertThat(report.paired()).isEqualTo(1);
        assertThat(report.findings()).extracting(EventPairingAudit.Finding::kind)
                .containsExactly("UNPAIRED_REQUEST", "UNPAIRED_RESPONSE");
        assertThat(report.findings().get(0).spanId()).isEqualTo("sp2");
        assertThat(report.findings().get(1).spanId()).isEqualTo("sp3");
    }

    @Test
    void hitlPairingAndEmptyAndNull() {
        EventPairingAudit.Report hitl = EventPairingAudit.audit(
                List.of(event("h1", "sp9", "HITL_REQUEST")),
                Map.of("HITL_REQUEST", "HITL_DECISION"));
        assertThat(hitl.findings()).hasSize(1);
        assertThat(hitl.findings().get(0).kind()).isEqualTo("UNPAIRED_REQUEST");

        assertThat(EventPairingAudit.audit(List.of(), Map.of("A", "B")).paired()).isZero();
        assertThatThrownBy(() -> EventPairingAudit.audit(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EventPairingAudit.audit(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
