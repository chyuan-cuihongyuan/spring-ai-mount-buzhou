package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-698 / spec 950：摘要存储水位——会话数 vs 上限、读面零行为变化。
 */
class SummaryWatermarkTest {

    private static StructuredSummary summary(String sessionId) {
        return new StructuredSummary(sessionId, 1L,
                Map.of("s", "内容"), 100, Instant.EPOCH);
    }

    @Test
    void watermarkTracksSessions() {
        InMemorySummaryStore store = new InMemorySummaryStore();
        store.save("s1", summary("s1"));
        store.save("s2", summary("s2"));

        InMemorySummaryStore.Watermark watermark = store.watermark();
        assertThat(watermark.activeSessions()).isEqualTo(2);
        assertThat(watermark.maxSessions()).isPositive(); // 默认配置有上限
    }

    @Test
    void readIsNonMutating() {
        InMemorySummaryStore store = new InMemorySummaryStore();
        store.save("s1", summary("s1"));
        var before = store.watermark();
        store.watermark();
        assertThat(store.watermark()).isEqualTo(before);
    }
}
