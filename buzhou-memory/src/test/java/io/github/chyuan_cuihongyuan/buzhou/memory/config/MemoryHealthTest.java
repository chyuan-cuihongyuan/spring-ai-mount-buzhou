package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 memory 健康：UP（state 探针往返）/ UNKNOWN（禁用）/
 * DOWN（存储抛异常）。探针不留残留（写后即删）。
 */
class MemoryHealthTest {

    @Test
    void upWithHealthyStores() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        MemoryHealth health = new MemoryHealth(true, stores);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.mechanism()).isEqualTo("memory");
        // 探针自清理
        assertThat(stores.sessionStateStore().getAll(MemoryHealth.PROBE_SESSION)).isEmpty();
    }

    @Test
    void unknownWhenDisabled() {
        assertThat(new MemoryHealth(false, Buzhou.inMemoryStores()).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
    }

    @Test
    void downWhenStoreBroken() {
        SessionStateStore broken = new SessionStateStore() {
            @Override
            public void put(String sessionId, StateEntry entry) {
                throw new IllegalStateException("down");
            }

            @Override
            public java.util.Optional<StateEntry> get(String sessionId, String key) {
                throw new IllegalStateException("down");
            }

            @Override
            public java.util.Map<String, StateEntry> getAll(String sessionId) {
                throw new IllegalStateException("down");
            }

            @Override
            public void delete(String sessionId, String key) {
                throw new IllegalStateException("down");
            }

            @Override
            public boolean deleteIfValueMatches(String sessionId, String key,
                    String expectedValue) {
                throw new IllegalStateException("down");
            }
        };
        BuzhouStores stores = new BuzhouStores(null, null, broken, null, null, null);
        assertThat(new MemoryHealth(true, stores).status()).isEqualTo(BuzhouHealth.Status.DOWN);
    }
}
