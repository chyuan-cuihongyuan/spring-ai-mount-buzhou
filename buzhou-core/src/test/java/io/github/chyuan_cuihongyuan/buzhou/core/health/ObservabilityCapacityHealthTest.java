package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 729 / T1058–T1059：观测容量健康面——used/max/utilization/evicted、
 * 逐出计数、null fail-fast。
 */
class ObservabilityCapacityHealthTest {

    @Test
    void detailsReflectUsageAndEvictions() {
        InMemoryObservabilityStore store = new InMemoryObservabilityStore(
                new InMemoryStoreConfig(null, null, 2, null));
        for (int i = 0; i < 3; i++) {
            store.saveSpans(List.of(new SpanRecord("s" + i, null, "sess-" + i, 0, "TOOL", "op",
                    Instant.parse("2026-09-13T00:00:0" + i + "Z"),
                    Instant.parse("2026-09-13T00:00:0" + i + "Z"), "OK", null)));
        }
        ObservabilityCapacityHealth health = new ObservabilityCapacityHealth(store);
        assertThat(health.mechanism()).isEqualTo("memory-observability");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        Map<String, Object> details = health.details();
        assertThat(details.get("used")).isEqualTo(2L); // 容量 2——第三会话顶掉最老
        assertThat(details.get("max")).isEqualTo(2L);
        assertThat(details.get("utilization")).isEqualTo(1.0);
        assertThat(details.get("evicted")).isEqualTo(1L);
    }

    @Test
    void nullStoreFailsFast() {
        assertThatThrownBy(() -> new ObservabilityCapacityHealth(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
