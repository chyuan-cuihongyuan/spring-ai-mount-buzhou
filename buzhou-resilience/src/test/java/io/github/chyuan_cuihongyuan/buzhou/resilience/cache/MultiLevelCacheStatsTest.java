package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1830 / T2862：多级缓存读面——逐层命中率、L1 失职率、联合命中率。 */
class MultiLevelCacheStatsTest {

    /** 三率分读：L1 撑门面、L2 兜底、miss 回源。 */
    @Test
    void shouldReadThreeRatesSeparately() {
        MultiLevelCacheStats.CacheReport r = MultiLevelCacheStats.report(60, 30, 10);
        assertThat(r.requests()).isEqualTo(100);
        assertThat(r.l1HitRate()).isEqualTo(0.6d);
        assertThat(r.combinedHitRate()).isEqualTo(0.9d);
        assertThat(r.l1DerelictionRate()).isEqualTo(0.3333333333333333d);
    }

    /** 极端三档：全 L1（健康）/ 全 L2（L1 形同虚设）/ 全 miss（缓存无效）。 */
    @Test
    void threeExtremesReadDistinctly() {
        MultiLevelCacheStats.CacheReport allL1 = MultiLevelCacheStats.report(100, 0, 0);
        assertThat(allL1.l1HitRate()).isEqualTo(1.0d);
        assertThat(allL1.l1DerelictionRate()).isZero();

        MultiLevelCacheStats.CacheReport allL2 = MultiLevelCacheStats.report(0, 100, 0);
        assertThat(allL2.l1DerelictionRate()).isEqualTo(1.0d);

        MultiLevelCacheStats.CacheReport allMiss = MultiLevelCacheStats.report(0, 0, 100);
        assertThat(allMiss.combinedHitRate()).isZero();
        assertThat(allMiss.l1DerelictionRate()).isEqualTo(-1d);
    }

    /** 零请求哨兵：三率全 -1。 */
    @Test
    void zeroRequestsYieldSentinels() {
        MultiLevelCacheStats.CacheReport r = MultiLevelCacheStats.report(0, 0, 0);
        assertThat(r.requests()).isZero();
        assertThat(r.l1HitRate()).isEqualTo(-1d);
        assertThat(r.combinedHitRate()).isEqualTo(-1d);
        assertThat(r.l1DerelictionRate()).isEqualTo(-1d);
    }

    /** 畸形账 fail-fast：负数与四路失恒。 */
    @Test
    void malformedAccountingFailsFast() {
        assertThatThrownBy(() -> MultiLevelCacheStats.report(-1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("四路合计=requests");
        assertThatThrownBy(() -> new MultiLevelCacheStats.CacheReport(100, 50, 40, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
