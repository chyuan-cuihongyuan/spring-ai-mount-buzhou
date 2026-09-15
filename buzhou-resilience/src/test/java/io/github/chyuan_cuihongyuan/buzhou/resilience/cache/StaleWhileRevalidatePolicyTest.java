package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1824 / T2850：SWR 三态——新鲜/陈旧异步刷新/过期，陈旧度读数。 */
class StaleWhileRevalidatePolicyTest {

    /** 三态判定与边界归属：达 fresh 进陈旧、达总寿过期。 */
    @Test
    void shouldClassifyThreeServingStates() {
        long fresh = 1000L;
        long staleWindow = 500L;
        assertThat(StaleWhileRevalidatePolicy.serving(500L, fresh, staleWindow))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.FRESH);
        assertThat(StaleWhileRevalidatePolicy.serving(1000L, fresh, staleWindow))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.STALE);
        assertThat(StaleWhileRevalidatePolicy.serving(1499L, fresh, staleWindow))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.STALE);
        assertThat(StaleWhileRevalidatePolicy.serving(1500L, fresh, staleWindow))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.EXPIRED);
    }

    /** 陈旧度读数：新鲜钳 0（不与哨兵撞值）、窗内 (0,1)、过期 ≥1。 */
    @Test
    void stalenessReadoutScalesAcrossWindows() {
        assertThat(StaleWhileRevalidatePolicy.staleness(500, 1000, 500))
                .isZero();
        assertThat(StaleWhileRevalidatePolicy.staleness(1250, 1000, 500))
                .isEqualTo(0.5d);
        assertThat(StaleWhileRevalidatePolicy.staleness(2000, 1000, 500))
                .isEqualTo(2.0d);
        // 无新鲜窗：哨兵
        assertThat(StaleWhileRevalidatePolicy.staleness(10, 0, 500))
                .isEqualTo(-1d);
    }

    /** 零陈旧窗退化：达 fresh 即过期（纯 TTL 语义兼容）。 */
    @Test
    void zeroStaleWindowDegradesToPureTtl() {
        assertThat(StaleWhileRevalidatePolicy.serving(999, 1000, 0))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.FRESH);
        assertThat(StaleWhileRevalidatePolicy.serving(1000, 1000, 0))
                .isEqualTo(StaleWhileRevalidatePolicy.Serving.EXPIRED);
    }

    /** 畸形入参 fail-fast：任一负值。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> StaleWhileRevalidatePolicy.serving(-1, 1000, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入参不能为负");
        assertThatThrownBy(() -> StaleWhileRevalidatePolicy.serving(1, -1, 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StaleWhileRevalidatePolicy.serving(1, 1000, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
