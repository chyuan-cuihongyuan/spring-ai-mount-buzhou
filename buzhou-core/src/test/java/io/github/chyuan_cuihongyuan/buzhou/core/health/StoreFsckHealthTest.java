package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 548 / T827：fsck 巡检健康面——观测面恒 UP、details 聚合读数
 * （runs/totalFindings/lastFindings）、未巡检 lastFindings=-1。
 */
class StoreFsckHealthTest {

    @Test
    void healthDetailsReflectHousekeeperReadings() {
        var keeper = new io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper(
                Buzhou.inMemoryStores(), null, Duration.ofHours(1));
        StoreFsckHealth health = new StoreFsckHealth(keeper);

        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details().get("lastFindings")).isEqualTo(-1); // 尚未巡检

        keeper.evaluateOnce();
        assertThat(health.details().get("runs")).isEqualTo(1L);
        assertThat(health.details().get("lastFindings")).isEqualTo(0); // 干净 store
    }

    @Test
    void nullKeeperFailsFast() {
        assertThatThrownBy(() -> new StoreFsckHealth(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
