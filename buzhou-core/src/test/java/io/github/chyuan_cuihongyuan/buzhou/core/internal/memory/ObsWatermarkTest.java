package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-677 / spec 924：观测存储水位读面——activeSessions/totalRecords 与实际
 * 表一致、上限字段直通配置、逐出计数透传、读面零行为变化。
 */
class ObsWatermarkTest {

    private static SpanRecord span(String session) {
        return new SpanRecord("sp-" + session, null, session, 1, "TOOL", "t",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), "OK", Map.of());
    }

    @Test
    void watermarkReflectsStoreState() {
        InMemoryStoreConfig config = new InMemoryStoreConfig(null, null, 50, 20);
        InMemoryObservabilityStore store = new InMemoryObservabilityStore(config);

        store.saveSpans(List.of(span("s1"), span("s1"), span("s2")));
        store.saveEvents(List.of(event("s1")));

        InMemoryObservabilityStore.Watermark watermark = store.watermark();
        assertThat(watermark.activeSessions()).isEqualTo(2); // s1 + s2
        assertThat(watermark.totalRecords()).isEqualTo(4); // 2 span + 1 span + 1 event
        assertThat(watermark.maxSessions()).isEqualTo(50);
        assertThat(watermark.maxRecordsPerSession()).isEqualTo(20);
        assertThat(watermark.sessionsEvicted()).isZero();
    }

    @Test
    void readsAreNonMutating() {
        InMemoryStoreConfig config = new InMemoryStoreConfig(null, null, 50, 20);
        InMemoryObservabilityStore store = new InMemoryObservabilityStore(config);
        store.saveSpans(List.of(span("s1")));

        InMemoryObservabilityStore.Watermark before = store.watermark();
        store.watermark(); // 再读一次
        InMemoryObservabilityStore.Watermark after = store.watermark();
        assertThat(after).isEqualTo(before); // 纯读面（读与读之间状态不变）
    }

    private static EventRecord event(String session) {
        return new EventRecord("s-ev", null, session, "user.turn.completed",
                Instant.EPOCH, Map.of());
    }
}
