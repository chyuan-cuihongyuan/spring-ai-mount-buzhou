package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-678 / spec 925：会话索引存量水位——条目数随 upsert/delete 正确变化、
 * 无界语义显式（maxSessions=-1）、读面零行为变化。
 */
class IndexWatermarkTest {

    private static SessionInfo info(String sessionId) {
        return new SessionInfo(sessionId, "app", "agent",
                SessionInfo.STATUS_ACTIVE, Instant.EPOCH.toEpochMilli(),
                Instant.EPOCH.toEpochMilli(), 1, Map.of());
    }

    @Test
    void watermarkTracksIndexSize() {
        InMemorySessionIndexStore store = new InMemorySessionIndexStore();

        assertThat(store.watermark().indexedSessions()).isZero();
        store.upsert(info("s1"));
        store.upsert(info("s2"));
        assertThat(store.watermark().indexedSessions()).isEqualTo(2);
        store.delete("s1");
        assertThat(store.watermark().indexedSessions()).isEqualTo(1);
    }

    @Test
    void unboundedSemanticsExplicit() {
        InMemorySessionIndexStore store = new InMemorySessionIndexStore();
        // 本实现无独立上限——maxSessions=-1 显式无界（诚实口径，spec 925 入档）
        assertThat(store.watermark().maxSessions()).isEqualTo(-1);
    }

    @Test
    void readIsNonMutating() {
        InMemorySessionIndexStore store = new InMemorySessionIndexStore();
        store.upsert(info("s1"));
        InMemorySessionIndexStore.Watermark before = store.watermark();
        store.watermark();
        assertThat(store.watermark()).isEqualTo(before);
    }
}
