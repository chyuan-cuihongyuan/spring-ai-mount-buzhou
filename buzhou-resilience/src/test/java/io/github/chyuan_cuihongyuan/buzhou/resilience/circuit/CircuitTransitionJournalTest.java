package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 702 / T1004–T1005：断路器变迁事件流读数——真状态机驱动（OPEN→HALF_OPEN→CLOSED
 * 三段变迁+聚合对齐）、环形覆盖 dropped、snapshot 防御拷贝+null fail-fast。
 */
class CircuitTransitionJournalTest {

    private static final Consumer<SessionEvent> SINK = e -> {
    };

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-12T00:00:00Z");

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void fullTransitionChainIsJournaledWithAggregates() {
        MutableClock clock = new MutableClock();
        ModelCircuitBreaker breaker = new ModelCircuitBreaker(new ResilienceProperties.Circuit(
                null, 10, 3, 0.5, Duration.ofMillis(100), null, null, 1), null, clock);
        CircuitTransitionJournal journal = breaker.transitionJournal();

        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK);
        breaker.recordTerminal("m", "NETWORK", SINK); // 3 失败 ≥ minCalls 且失败率 1.0 → OPEN
        assertThat(breaker.state("m")).isEqualTo(CircuitState.OPEN);

        CircuitTransitionJournal.Report afterTrip = journal.snapshot();
        assertThat(afterTrip.recent()).hasSize(1);
        assertThat(afterTrip.recent().get(0).to()).isEqualTo("OPEN");
        assertThat(afterTrip.recent().get(0).consecutiveTrips()).isEqualTo(1);
        assertThat(afterTrip.tripsByModel()).containsEntry("m", 1L);

        clock.advanceMillis(200); // 冷却耗尽 → 探测放行即 HALF_OPEN
        breaker.beforeCall("m", SINK);
        assertThat(breaker.state("m")).isEqualTo(CircuitState.HALF_OPEN);
        assertThat(journal.snapshot().halfOpensByModel()).containsEntry("m", 1L);

        breaker.recordSuccess("m", SINK); // 半开阈值 1 → CLOSED
        assertThat(breaker.state("m")).isEqualTo(CircuitState.CLOSED);

        CircuitTransitionJournal.Report report = journal.snapshot();
        assertThat(report.recent()).hasSize(3); // OPEN→HALF_OPEN→CLOSED 全链
        assertThat(report.recent().get(0).to()).isEqualTo("CLOSED");
        assertThat(report.recent().get(0).from()).isEqualTo("HALF_OPEN");
        assertThat(report.recent().get(2).from()).isEqualTo("CLOSED");
        assertThat(report.recoveriesByModel()).containsEntry("m", 1L);
        assertThat(report.tripsByModel()).containsEntry("m", 1L);
        assertThat(report.dropped()).isZero();
    }

    @Test
    void ringCapacityEvictsOldestAndCountsDropped() {
        CircuitTransitionJournal journal = new CircuitTransitionJournal(2);
        journal.record("m", "CLOSED", "OPEN", 1, 1, 100);
        journal.record("m", "OPEN", "HALF_OPEN", 2, -1, -1);
        journal.record("m", "HALF_OPEN", "CLOSED", 3, -1, -1);
        CircuitTransitionJournal.Report report = journal.snapshot();
        assertThat(report.capacity()).isEqualTo(2);
        assertThat(report.dropped()).isEqualTo(1);
        assertThat(report.recent()).hasSize(2);
        assertThat(report.recent().get(0).atEpochMs()).isEqualTo(3); // 最新在前
        assertThat(report.recent().get(1).atEpochMs()).isEqualTo(2);
        assertThat(report.tripsByModel()).containsEntry("m", 1L); // 聚合不受环形覆盖影响
        assertThat(report.recoveriesByModel()).containsEntry("m", 1L);
    }

    @Test
    void snapshotIsImmutableAndNullsFailFast() {
        CircuitTransitionJournal journal = new CircuitTransitionJournal();
        assertThatThrownBy(() -> journal.record(null, "CLOSED", "OPEN", 1, 1, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> journal.record("m", null, "OPEN", 1, 1, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> journal.record("m", "CLOSED", null, 1, 1, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CircuitTransitionJournal(0))
                .isInstanceOf(IllegalArgumentException.class);
        journal.record("m", "CLOSED", "OPEN", 1, 1, 100);
        CircuitTransitionJournal.Report report = journal.snapshot();
        new ArrayList<>(report.recent()).clear();
        assertThat(journal.snapshot().recent()).hasSize(1); // 防御拷贝
        assertThat(report.recent().get(0).openDurationMs()).isEqualTo(100);
    }
}
