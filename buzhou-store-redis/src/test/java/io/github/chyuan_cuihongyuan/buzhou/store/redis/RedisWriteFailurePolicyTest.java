package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 13 §stores-7 / ticket 32：写失败策略双轨契约（Redis 侧与 JDBC 侧同语义）——
 * FAIL_TURN 观测类写失败原样抛；DEGRADE 降级 WARN + 计数继续（不静默）。
 */
class RedisWriteFailurePolicyTest {

    /** 全写必炸的桩（模拟存储故障窗口）。 */
    private static final class FailingObservabilityStore implements ObservabilityStore {

        @Override
        public void saveSpans(List<SpanRecord> spans) {
            throw new IllegalStateException("simulated span write failure");
        }

        @Override
        public void saveEvents(List<EventRecord> events) {
            throw new IllegalStateException("simulated event write failure");
        }

        @Override
        public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
            throw new IllegalStateException("simulated snapshot write failure");
        }

        @Override
        public List<SpanRecord> spansOfSession(String sessionId) {
            return List.of();
        }

        @Override
        public List<EventRecord> eventsOfSession(String sessionId) {
            return List.of();
        }

        @Override
        public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
            return Optional.empty();
        }

        @Override
        public List<SessionSummary> listSessionSummaries(String cursor, int size) {
            return List.of();
        }

        @Override
        public List<EventRecord> eventsOfSpan(String spanId) {
            return List.of();
        }
    }

    private final ObservabilityStore failing = new FailingObservabilityStore();

    @Test
    void shouldPropagateWriteFailure_whenPolicyIsFailTurn() {
        DegradingObservabilityStore store = new DegradingObservabilityStore(failing,
                WriteFailurePolicy.FAIL_TURN);

        assertThatThrownBy(() -> store.saveSpans(List.of(span())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store.saveEvents(List.of(event())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(store.degradedWriteCount()).isZero();
    }

    @Test
    void shouldDegradeAndCount_whenPolicyIsDegrade() {
        DegradingObservabilityStore store = new DegradingObservabilityStore(failing,
                WriteFailurePolicy.DEGRADE);

        store.saveSpans(List.of(span()));   // 不抛：观测类写降级
        store.saveEvents(List.of(event()));
        store.saveInjectionSnapshot(snapshot());

        assertThat(store.degradedWriteCount()).isEqualTo(3L); // 降级不静默（计数可断言）
        assertThat(store.spansOfSession("s")).isEmpty();      // 读路径不受影响
    }

    private SpanRecord span() {
        return new SpanRecord("sp-1", null, "s", 1, "Turn", "turn-1",
                Instant.now(), null, "RUNNING", Map.of());
    }

    private EventRecord event() {
        return new EventRecord("ev-1", "sp-1", "s", "Thinking", Instant.now(), Map.of());
    }

    private InjectionSnapshot snapshot() {
        return new InjectionSnapshot("s", 1, List.of("m1"), Map.of(), Instant.now());
    }
}
