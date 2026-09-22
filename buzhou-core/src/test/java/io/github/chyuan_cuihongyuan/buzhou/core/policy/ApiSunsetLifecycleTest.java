package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1916 / T3034：日落生命周期——三段、边界、剩余、畸形。 */
class ApiSunsetLifecycleTest {

    private static final long DEPRECATE = 1_000_000L;
    private static final long SUNSET = 2_000_000L;

    /** 三段：公告前 ACTIVE / 公告期 DEPRECATED / 日落后 SUNSET。 */
    @Test
    void threePhases() {
        assertThat(ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, 500_000))
                .isEqualTo(ApiSunsetLifecycle.Phase.ACTIVE);
        assertThat(ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, 1_500_000))
                .isEqualTo(ApiSunsetLifecycle.Phase.DEPRECATED);
        assertThat(ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, 2_500_000))
                .isEqualTo(ApiSunsetLifecycle.Phase.SUNSET);
    }

    /** 边界含上：恰公告期即 DEPRECATED、恰日落即 SUNSET。 */
    @Test
    void boundariesInclusive() {
        assertThat(ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, DEPRECATE))
                .isEqualTo(ApiSunsetLifecycle.Phase.DEPRECATED);
        assertThat(ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, SUNSET))
                .isEqualTo(ApiSunsetLifecycle.Phase.SUNSET);
    }

    /** 剩余时间：正数直读、已日落钳 0。 */
    @Test
    void daysRemainingClamped() {
        assertThat(ApiSunsetLifecycle.daysRemainingMillis(SUNSET, 1_500_000))
                .isEqualTo(500_000);
        assertThat(ApiSunsetLifecycle.daysRemainingMillis(SUNSET, 3_000_000))
                .isZero();
    }

    /** 畸形入参 fail-fast：日落早于弃用、负 now。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ApiSunsetLifecycle.phase(2_000_000, 1_000_000, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ deprecatedAt ≤ sunsetAt");
        assertThatThrownBy(() -> ApiSunsetLifecycle.phase(DEPRECATE, SUNSET, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("now 不能为负");
    }
}
