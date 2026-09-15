package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1855 / T2912：收藏家期望——k·H(k) 全量、余轮递减、哨兵。 */
class CouponCollectorProjectionTest {

    /** 已知值：k=1 → 1；k=2 → 2×1.5=3；k=3 → 3×(11/6)=5.5。 */
    @Test
    void knownSmallValuesHold() {
        assertThat(CouponCollectorProjection.expectedDraws(1)).isEqualTo(1.0d);
        assertThat(CouponCollectorProjection.expectedDraws(2)).isEqualTo(3.0d);
        assertThat(CouponCollectorProjection.expectedDraws(3)).isEqualTo(5.5d);
        assertThat(CouponCollectorProjection.expectedDraws(0)).isZero();
    }

    /** 长尾性质：k=10 期望 ≈ 29.3 轮——收齐远多于类数（最后一类最贵）。 */
    @Test
    void longTailExceedsKindCount() {
        double expected = CouponCollectorProjection.expectedDraws(10);
        assertThat(expected).isGreaterThan(29d).isLessThan(30d);
        assertThat(expected).isGreaterThan(10);
    }

    /** 余轮：全见 0；零见 = 全量期望；单调递减。 */
    @Test
    void remainingDecreasesWithProgress() {
        double all = CouponCollectorProjection.expectedRemaining(10, 10);
        double none = CouponCollectorProjection.expectedRemaining(0, 10);
        double half = CouponCollectorProjection.expectedRemaining(5, 10);
        assertThat(all).isZero();
        assertThat(none).isEqualTo(CouponCollectorProjection.expectedDraws(10));
        assertThat(half).isBetween(0d, none);
        assertThat(half).isLessThan(none);
    }

    /** 畸形入参 fail-fast：负类数、进度越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> CouponCollectorProjection.expectedDraws(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalKinds 不能为负");
        assertThatThrownBy(() -> CouponCollectorProjection.expectedRemaining(6, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ seen ≤ total");
        assertThatThrownBy(() -> CouponCollectorProjection.expectedRemaining(-1, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
