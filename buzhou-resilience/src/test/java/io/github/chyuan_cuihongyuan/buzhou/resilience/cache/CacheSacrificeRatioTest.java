package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1866 / T2934：牺牲率——插入驱逐比、颠簸双条件、哨兵。 */
class CacheSacrificeRatioTest {

    /** 全联理想：零牺牲 + 高命中 = 不颠簸。 */
    @Test
    void fullyAssociativeIdealReadsHealthy() {
        CacheSacrificeRatio.CacheAccount ideal =
                new CacheSacrificeRatio.CacheAccount(100, 0, 90, 10);
        assertThat(ideal.sacrificeRatio()).isZero();
        assertThat(ideal.hitRate()).isEqualTo(0.9d);
        assertThat(ideal.thrashing(0.5, 0.5)).isFalse();
    }

    /** 颠簸：牺牲 0.8 + 命中 0.3 双过 → 白忙（插入的还没用就被挤走）。 */
    @Test
    void thrashingNeedsBothConditions() {
        CacheSacrificeRatio.CacheAccount thrashy =
                new CacheSacrificeRatio.CacheAccount(100, 80, 30, 70);
        assertThat(thrashy.sacrificeRatio()).isEqualTo(0.8d);
        assertThat(thrashy.hitRate()).isEqualTo(0.3d);
        assertThat(thrashy.thrashing(0.5, 0.5)).isTrue();

        // 满缓存正常换血：牺牲高但命中也高——非颠簸
        CacheSacrificeRatio.CacheAccount churning =
                new CacheSacrificeRatio.CacheAccount(100, 80, 90, 10);
        assertThat(churning.thrashing(0.5, 0.5)).isFalse();
    }

    /** 哨兵：零插入牺牲率 -1；零查找命中率 -1。 */
    @Test
    void sentinelsBehave() {
        CacheSacrificeRatio.CacheAccount noInserts =
                new CacheSacrificeRatio.CacheAccount(0, 0, 5, 5);
        assertThat(noInserts.sacrificeRatio()).isEqualTo(-1d);
        assertThat(noInserts.hitRate()).isEqualTo(0.5d);

        CacheSacrificeRatio.CacheAccount noLookups =
                new CacheSacrificeRatio.CacheAccount(10, 2, 0, 0);
        assertThat(noLookups.hitRate()).isEqualTo(-1d);
        assertThat(noLookups.thrashing(0.1, 0.5)).isFalse(); // -1 < 0.5 不触发
    }

    /** 畸形入参 fail-fast：负计数、阈值越界/NaN。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new CacheSacrificeRatio.CacheAccount(-1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法缓存账");
        assertThatThrownBy(() -> new CacheSacrificeRatio.CacheAccount(1, 1, 1, 1)
                .thrashing(1.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("阈值须在 [0,1]");
        assertThatThrownBy(() -> new CacheSacrificeRatio.CacheAccount(1, 1, 1, 1)
                .thrashing(0.5, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
