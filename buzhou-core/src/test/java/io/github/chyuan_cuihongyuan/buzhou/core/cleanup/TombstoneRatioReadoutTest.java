package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1850 / T2902：墓碑占比——空间账、读放大、压实阈。 */
class TombstoneRatioReadoutTest {

    /** 占比与读放大：0.5 占比 → 2 倍读放大。 */
    @Test
    void shouldComputeRatioAndAmplification() {
        TombstoneRatioReadout.Ratio r = TombstoneRatioReadout.ratioOf(50, 50);
        assertThat(r.ratio()).isEqualTo(0.5d);
        assertThat(r.readAmplification()).isEqualTo(2.0d);
        assertThat(r.shouldCompact(0.2d)).isTrue();

        TombstoneRatioReadout.Ratio healthy = TombstoneRatioReadout.ratioOf(99, 1);
        assertThat(healthy.ratio()).isEqualTo(0.01d);
        assertThat(healthy.readAmplification()).isCloseTo(1.0101d,
                org.assertj.core.data.Offset.offset(1e-4));
        assertThat(healthy.shouldCompact(0.2d)).isFalse();
    }

    /** 边界：全空 ratio 0 放大 1；全墓碑 ratio 1 放大无穷。 */
    @Test
    void boundariesBehave() {
        TombstoneRatioReadout.Ratio empty = TombstoneRatioReadout.ratioOf(0, 0);
        assertThat(empty.ratio()).isZero();
        assertThat(empty.readAmplification()).isEqualTo(1.0d);

        TombstoneRatioReadout.Ratio allDead = TombstoneRatioReadout.ratioOf(0, 10);
        assertThat(allDead.ratio()).isEqualTo(1.0d);
        assertThat(allDead.readAmplification()).isInfinite();
    }

    /** 压实阈边界含上：ratio == threshold 即该压。 */
    @Test
    void compactThresholdIsInclusive() {
        TombstoneRatioReadout.Ratio r = TombstoneRatioReadout.ratioOf(80, 20);
        assertThat(r.ratio()).isEqualTo(0.2d);
        assertThat(r.shouldCompact(0.2d)).isTrue();
        assertThat(r.shouldCompact(0.21d)).isFalse();
    }

    /** 畸形入参 fail-fast：负计数、阈值越界/NaN。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TombstoneRatioReadout.ratioOf(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入参不能为负");
        assertThatThrownBy(() -> TombstoneRatioReadout.ratioOf(1, 1).shouldCompact(1.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 须在 [0,1]");
        assertThatThrownBy(() -> TombstoneRatioReadout.ratioOf(1, 1).shouldCompact(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
