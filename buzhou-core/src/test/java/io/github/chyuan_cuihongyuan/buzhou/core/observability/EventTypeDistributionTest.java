package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 726 / T1052–T1053：事件类型分布——降序+字典序稳定、topType 占比、
 * 空表、null fail-fast。
 */
class EventTypeDistributionTest {

    private static EventRecord event(String type) {
        return new EventRecord("e-" + type, "span-1", "sess", type,
                Instant.parse("2026-09-13T00:00:00Z"), Map.of());
    }

    @Test
    void countsSortDescendingWithStableTies() {
        EventTypeDistribution.Report report = EventTypeDistribution.of(List.of(
                event("circuit.call-rejected"),
                event("circuit.call-rejected"),
                event("circuit.call-rejected"),
                event("turn.completed"),
                event("turn.completed"),
                event("context.low-watermark")));
        assertThat(report.total()).isEqualTo(6);
        assertThat(report.distinctTypes()).isEqualTo(3);
        assertThat(report.rows()).extracting(EventTypeDistribution.Row::type)
                .containsExactly("circuit.call-rejected", "turn.completed", "context.low-watermark");
        assertThat(report.top().type()).isEqualTo("circuit.call-rejected");
        assertThat(report.top().count()).isEqualTo(3);
    }

    @Test
    void emptyAndNullAndCustomTypes() {
        EventTypeDistribution.Report empty = EventTypeDistribution.of(List.of());
        assertThat(empty.total()).isZero();
        assertThat(empty.top()).isNull();
        assertThat(empty.rows()).isEmpty();
        assertThatThrownBy(() -> EventTypeDistribution.of(null))
                .isInstanceOf(NullPointerException.class);
        EventTypeDistribution.Report custom = EventTypeDistribution.of(List.of(event("my.custom")));
        assertThat(custom.rows().get(0).type()).isEqualTo("my.custom");
    }
}
