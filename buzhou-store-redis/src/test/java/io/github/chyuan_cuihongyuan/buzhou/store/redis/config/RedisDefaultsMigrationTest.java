package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-42 / spec 13 §T68：redis 快照 TTL 默认 PT168H（迁移自 ZERO 永生）——
 * 快照不再无界堆积；显式 ZERO 仍表示永不过期（兼容语义）；负池大小启动即拒。
 */
class RedisDefaultsMigrationTest {

    @Test
    void snapshotTtlDefaultsToSevenDays() {
        RedisStoreProperties properties = new RedisStoreProperties(null, null, null, null);
        assertThat(properties.snapshotTtl()).isEqualTo(Duration.ofHours(168));
    }

    @Test
    void explicitZeroStillMeansNoExpiry() {
        assertThat(new RedisStoreProperties(null, null, Duration.ZERO, null).snapshotTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(new RedisStoreProperties(null, null, Duration.ofHours(2), null).snapshotTtl())
                .isEqualTo(Duration.ofHours(2));
    }

    @Test
    void negativePoolSizeRejected() {
        assertThatThrownBy(() -> new RedisStoreProperties(null, null, null, -3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pool-max-size");
    }
}
