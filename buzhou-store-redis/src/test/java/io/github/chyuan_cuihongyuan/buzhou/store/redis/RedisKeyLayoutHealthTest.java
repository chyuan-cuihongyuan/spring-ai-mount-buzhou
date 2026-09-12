package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 727 / T1054–T1055：Redis 键布局健康面——恒 UP、details 聚合三族计数、
 * 保留段清单。
 */
class RedisKeyLayoutHealthTest {

    @Test
    void healthIsUpWithCollisionDetails() {
        RedisKeyLayoutHealth health = new RedisKeyLayoutHealth("buzhou:");
        assertThat(health.mechanism()).isEqualTo("redis-key-layout");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("findings", 9L);
        assertThat(health.details()).containsEntry("shapeCollisions", 2L);
        assertThat(health.details()).containsEntry("colonSuffixTricks", 1L);
        assertThat((int) health.details().get("reservedSegments")).isGreaterThanOrEqualTo(3);
    }

    @Test
    void customPrefixStillUpWithFindings() {
        RedisKeyLayoutHealth health = new RedisKeyLayoutHealth("x:");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details().get("findings")).isEqualTo(9L);
    }
}
