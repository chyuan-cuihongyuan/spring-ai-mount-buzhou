package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 810 / T1122：存储延迟环回归——样本环 FIFO/P50 P95 最近秩/装饰器透传
 * 计时+异常照记/操作名封顶/参数防御。
 */
class StoreLatencyRingTest {

    @Test
    void ringSamplesAndPercentiles() {
        StoreLatencyRing ring = new StoreLatencyRing();
        for (long ms = 1; ms <= 100; ms++) {
            ring.record("append", ms);
        }
        StoreLatencyRing.OpStats stats = ring.stats("append");
        assertThat(stats.count()).isEqualTo(100);
        assertThat(stats.totalMillis()).isEqualTo(5050);
        assertThat(stats.maxMillis()).isEqualTo(100);
        assertThat(stats.p50Millis()).isEqualTo(50);
        assertThat(stats.p95Millis()).isEqualTo(95);
        assertThat(stats.ringFull()).isFalse();
    }

    @Test
    void ringEvictsOldestWhenFull() {
        StoreLatencyRing ring = new StoreLatencyRing();
        for (long ms = 1; ms <= StoreLatencyRing.RING_CAPACITY + 10; ms++) {
            ring.record("load", ms);
        }
        StoreLatencyRing.OpStats stats = ring.stats("load");
        assertThat(stats.count()).isEqualTo(StoreLatencyRing.RING_CAPACITY);
        assertThat(stats.ringFull()).isTrue();
        // 灌入 1..138，最老 1..10 被挤——环=11..138 升序
        assertThat(stats.p50Millis()).isEqualTo(74);  // rank=⌈0.5·128⌉=64 → 11+63
        assertThat(stats.p95Millis()).isEqualTo(132); // rank=⌈0.95·128⌉=122 → 11+121
    }

    @Test
    void unknownOpAndInvalidInputs() {
        StoreLatencyRing ring = new StoreLatencyRing();
        assertThat(ring.stats("nope")).isNull();
        ring.record(null, 5);
        ring.record("  ", 5);
        ring.record("append", -1);
        assertThat(ring.stats("append")).isNull();
        assertThat(ring.recorded()).isZero();
    }

    @Test
    void opNamesAreCapped() {
        StoreLatencyRing ring = new StoreLatencyRing();
        for (int i = 0; i < StoreLatencyRing.MAX_OPS + 5; i++) {
            ring.record("op" + i, 1);
        }
        assertThat(ring.truncated()).isTrue();
        assertThat(ring.all()).hasSize(StoreLatencyRing.MAX_OPS);
        assertThat(ring.recorded()).isEqualTo(StoreLatencyRing.MAX_OPS + 5);
    }

    @Test
    void decoratorRecordsAllThreeOpsAndPassesThrough() {
        StoreLatencyRing ring = new StoreLatencyRing();
        MessageStoreStub stub = new MessageStoreStub();
        TimedMessageStore timed = new TimedMessageStore(stub, ring);

        timed.append("s1", List.of());
        timed.load("s1");
        timed.findById("m1");

        assertThat(stub.appendCalls).isEqualTo(1);
        assertThat(stub.loadCalls).isEqualTo(1);
        assertThat(stub.findCalls).isEqualTo(1);
        assertThat(ring.all()).hasSize(3);
        assertThat(ring.stats("append").count()).isEqualTo(1);
        assertThat(ring.stats("load").count()).isEqualTo(1);
        assertThat(ring.stats("findById").count()).isEqualTo(1);
    }

    @Test
    void decoratorRecordsLatencyOnExceptionToo() {
        StoreLatencyRing ring = new StoreLatencyRing();
        MessageStore boom = new MessageStore() {
            @Override
            public void append(String sessionId, List<BuzhouMessage> messages) {
                throw new IllegalStateException("disk full");
            }

            @Override
            public List<BuzhouMessage> load(String sessionId) {
                return List.of();
            }

            @Override
            public Optional<BuzhouMessage> findById(String messageId) {
                return Optional.empty();
            }
        };
        TimedMessageStore timed = new TimedMessageStore(boom, ring);
        try {
            timed.append("s1", List.of());
        } catch (IllegalStateException expected) {
            // 照抛
        }
        assertThat(ring.stats("append").count()).isEqualTo(1); // 异常也计时
    }

    @Test
    void failFastOnNulls() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> new TimedMessageStore(null, new StoreLatencyRing()));
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> new TimedMessageStore(new MessageStoreStub(), null));
    }

    /** 计数桩。 */
    private static final class MessageStoreStub implements MessageStore {
        int appendCalls;
        int loadCalls;
        int findCalls;

        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
            appendCalls++;
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            loadCalls++;
            return List.of();
        }

        @Override
        public Optional<BuzhouMessage> findById(String messageId) {
            findCalls++;
            return Optional.empty();
        }
    }
}
