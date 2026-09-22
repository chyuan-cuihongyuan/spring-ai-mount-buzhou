package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1918 / T3038：有界旧读——旧度、判定、偏斜、畸形。 */
class BoundedStalenessTest {

    /** 旧度读数：读比写晚 800 → 旧度 800。 */
    @Test
    void stalenessReadout() {
        assertThat(BoundedStaleness.stalenessMillis(1000, 1800)).isEqualTo(800);
    }

    /** 判定两态与边界含上：恰等于界仍达标。 */
    @Test
    void verdictBoundaryInclusive() {
        assertThat(BoundedStaleness.verdict(800, 1000))
                .isEqualTo(BoundedStaleness.Staleness.WITHIN_BOUND);
        assertThat(BoundedStaleness.verdict(1000, 1000))
                .isEqualTo(BoundedStaleness.Staleness.WITHIN_BOUND);
        assertThat(BoundedStaleness.verdict(1001, 1000))
                .isEqualTo(BoundedStaleness.Staleness.STALE);
    }

    /** 时钟偏斜：读早于写 → 旧度钳 0（负旧度无意义）。 */
    @Test
    void clockSkewClampedToZero() {
        assertThat(BoundedStaleness.stalenessMillis(1000, 800)).isZero();
        assertThat(BoundedStaleness.verdict(0, 100))
                .isEqualTo(BoundedStaleness.Staleness.WITHIN_BOUND);
    }

    /** 畸形入参 fail-fast：负时点、负界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> BoundedStaleness.stalenessMillis(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时点不能为负");
        assertThatThrownBy(() -> BoundedStaleness.verdict(100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bound 不能为负");
    }
}
