package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 4039 / T6080：DDSketch 合同——相对误差分位、min/max
 * 精确旁路、合并等价、γ 不一致/空草图/越界 fail-fast。
 */
class DdSketchTest {

    private static final double ALPHA = 0.01;

    private static DdSketch acceptRange(int from, int to) {
        DdSketch sketch = new DdSketch(ALPHA);
        for (int value = from; value <= to; value++) {
            sketch.accept(value);
        }
        return sketch;
    }

    @Test
    void quantilesShouldStayWithinRelativeAccuracy() {
        DdSketch sketch = acceptRange(1, 1000);
        assertThat(sketch.count()).isEqualTo(1000L);
        assertThat(sketch.quantile(0.5)).isCloseTo(500.0, within(500.0 * 0.025));   // ≈503
        assertThat(sketch.quantile(0.99)).isCloseTo(990.0, within(990.0 * 0.025));  // ≈995
        assertThat(sketch.quantile(1.0)).isCloseTo(1000.0, within(1000.0 * 0.025));
    }

    @Test
    void minMaxShouldBeExactBypass() {
        DdSketch sketch = acceptRange(1, 1000);
        assertThat(sketch.min()).isEqualTo(1.0);
        assertThat(sketch.max()).isEqualTo(1000.0);
    }

    @Test
    void mergeShouldEqualSingleBuild() {
        DdSketch merged = acceptRange(1, 500);
        merged.merge(acceptRange(501, 1000));
        DdSketch single = acceptRange(1, 1000);
        assertThat(merged.count()).isEqualTo(single.count());
        assertThat(merged.quantile(0.5)).isCloseTo(single.quantile(0.5), within(1e-9));
        assertThat(merged.quantile(0.95)).isCloseTo(single.quantile(0.95), within(1e-9));
        assertThat(merged.min()).isEqualTo(1.0);
        assertThat(merged.max()).isEqualTo(1000.0);
    }

    @Test
    void scaleInvarianceShouldGiveSameRelativeBucketWidth() {
        DdSketch small = new DdSketch(ALPHA);
        DdSketch large = new DdSketch(ALPHA);
        for (int i = 1; i <= 100; i++) {
            small.accept(i);
            large.accept(i * 1_000_000.0);   // 六个量级以上
        }
        double smallRatio = small.quantile(0.5) / 50.0;
        double largeRatio = large.quantile(0.5) / 50_000_000.0;
        assertThat(largeRatio).isCloseTo(smallRatio, within(0.02));   // 量级无关同精度
    }

    @Test
    void invalidValuesAndStatesShouldFailFast() {
        assertThatThrownBy(() -> new DdSketch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DdSketch(1.0)).isInstanceOf(IllegalArgumentException.class);
        DdSketch sketch = new DdSketch(ALPHA);
        assertThatThrownBy(() -> sketch.accept(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.accept(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.accept(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sketch.quantile(0.5)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sketch.quantile(0)).isInstanceOf(IllegalArgumentException.class);
        sketch.accept(1);
        assertThatThrownBy(() -> sketch.merge(new DdSketch(0.05)))   // γ 不一致
                .isInstanceOf(IllegalArgumentException.class);
    }
}
