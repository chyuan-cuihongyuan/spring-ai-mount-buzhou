package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2001 / T3104：HLL 基数素描合同——单元素精确（线性计数修正）、
 * 幂等（重复 offer 估计不变）、大基数误差带（≤ 3× 理论界）、合并并集
 * 语义、畸形五型 fail-fast、误差界/寄存器数换算面。
 */
class HllCardinalitySketchTest {

    @Test
    void singleElementShouldEstimateExactlyOne() {
        HllCardinalitySketch sketch = new HllCardinalitySketch();
        sketch.offer("session-a");
        assertThat(sketch.estimate()).isEqualTo(1L);
        assertThat(sketch.offeredCount()).isEqualTo(1L);
    }

    @Test
    void duplicateOffersShouldNotChangeEstimate() {
        HllCardinalitySketch sketch = new HllCardinalitySketch();
        for (int i = 0; i < 1000; i++) {
            sketch.offer("same-session");
        }
        assertThat(sketch.estimate()).isEqualTo(1L); // 幂等：distinct 仍 1
        assertThat(sketch.offeredCount()).isEqualTo(1000L); // 对账面：重复率 99.9%
    }

    @Test
    void smallDistinctSetShouldEstimateNearExactly() {
        HllCardinalitySketch sketch = new HllCardinalitySketch();
        for (int i = 0; i < 50; i++) {
            sketch.offer("tenant-" + i);
        }
        // 线性计数修正域（50 ≪ 2.5×4096）：误差应远小于 1
        assertThat(sketch.estimate()).isBetween(49L, 51L);
    }

    @Test
    void largeDistinctSetShouldStayWithinErrorBand() {
        HllCardinalitySketch sketch = new HllCardinalitySketch();
        int distinct = 20_000;
        for (int i = 0; i < distinct; i++) {
            sketch.offer("tool-invocation-" + i);
        }
        // 理论界 1.04/√4096 ≈ 1.6%；确定性 hash 下取 3× 宽容带 ≈ 5%
        double relative = Math.abs(sketch.estimate() - distinct) / (double) distinct;
        assertThat(relative).isLessThanOrEqualTo(0.05d);
    }

    @Test
    void mergeShouldEstimateUnion() {
        HllCardinalitySketch a = new HllCardinalitySketch();
        HllCardinalitySketch b = new HllCardinalitySketch();
        for (int i = 0; i < 5000; i++) {
            a.offer("shard-a-" + i);
            b.offer("shard-b-" + i);
        }
        long beforeMerge = a.estimate();
        a.merge(b);
        // 并集 ≈ 10000（合并语义：A.merge(B) ≈ A∪B，非相加）
        double relative = Math.abs(a.estimate() - 10_000L) / 10_000.0d;
        assertThat(relative).isLessThanOrEqualTo(0.05d);
        assertThat(a.estimate()).isGreaterThan(beforeMerge);
        assertThat(a.offeredCount()).isEqualTo(10_000L);
    }

    @Test
    void precisionAndRegisterCountShouldFollowPowerOfTwo() {
        assertThat(new HllCardinalitySketch(4).registerCount()).isEqualTo(16);
        assertThat(new HllCardinalitySketch(16).registerCount()).isEqualTo(65_536);
        // 误差界随寄存器数开方反比
        assertThat(new HllCardinalitySketch(12).relativeErrorBound())
                .isCloseTo(1.04d / Math.sqrt(4096), org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new HllCardinalitySketch(3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HllCardinalitySketch(17))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HllCardinalitySketch().offer(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HllCardinalitySketch(12).merge(new HllCardinalitySketch(13)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("精度不一致");
        assertThatThrownBy(() -> new HllCardinalitySketch().merge(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
