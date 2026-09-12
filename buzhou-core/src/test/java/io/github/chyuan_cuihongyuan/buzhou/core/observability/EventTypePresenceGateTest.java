package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 739 / T1078–T1079：事件静默缺失门——缺失清单/全满足/空期望/null。
 */
class EventTypePresenceGateTest {

    private static EventRecord event(String type) {
        return new EventRecord("e", "s", "sess", type,
                Instant.parse("2026-09-13T00:00:00Z"), Map.of());
    }

    @Test
    void missingTypesAreReported() {
        EventTypePresenceGate.Report report = EventTypePresenceGate.gate(
                List.of(event("session.started"), event("turn.completed")),
                Set.of("session.started", "session.finished", "turn.completed"));
        assertThat(report.missing()).containsExactly("session.finished");
        assertThat(report.expectedCount()).isEqualTo(3);
        assertThat(report.observedTypes()).isEqualTo(2);
    }

    @Test
    void allPresentAndEmptyContractAndNull() {
        EventTypePresenceGate.Report all = EventTypePresenceGate.gate(
                List.of(event("a"), event("b")), Set.of("a", "b"));
        assertThat(all.missing()).isEmpty();

        EventTypePresenceGate.Report none = EventTypePresenceGate.gate(
                List.of(event("a")), Set.of());
        assertThat(none.missing()).isEmpty(); // 无契约不误报

        assertThatThrownBy(() -> EventTypePresenceGate.gate(null, Set.of("x")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EventTypePresenceGate.gate(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
