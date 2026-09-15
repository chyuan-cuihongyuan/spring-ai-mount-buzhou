package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1840 / T2882：向量时钟——因果三态、稀疏缺席、相等退化。 */
class VectorClockOrderTest {

    /** 因果先序：各分量 ≤ 且至少一严格小。 */
    @Test
    void shouldDetectCausalBeforeAndAfter() {
        assertThat(VectorClockOrder.compare(
                Map.of("n1", 1L, "n2", 1L),
                Map.of("n1", 2L, "n2", 1L)))
                .isEqualTo(VectorClockOrder.CausalOrder.BEFORE);
        assertThat(VectorClockOrder.compare(
                Map.of("n1", 3L),
                Map.of("n1", 2L)))
                .isEqualTo(VectorClockOrder.CausalOrder.AFTER);
    }

    /** 并发：互相各有领先分量——冲突解判定前提。 */
    @Test
    void shouldDetectConcurrentDivergence() {
        assertThat(VectorClockOrder.compare(
                Map.of("n1", 2L, "n2", 1L),
                Map.of("n1", 1L, "n2", 2L)))
                .isEqualTo(VectorClockOrder.CausalOrder.CONCURRENT);
    }

    /** 稀疏缺席按 0：缺席方落后于在场方。 */
    @Test
    void absentComponentsCountAsZero() {
        assertThat(VectorClockOrder.compare(
                Map.of(), Map.of("n1", 1L)))
                .isEqualTo(VectorClockOrder.CausalOrder.BEFORE);
        assertThat(VectorClockOrder.compare(
                Map.of("n1", 1L), Map.of()))
                .isEqualTo(VectorClockOrder.CausalOrder.AFTER);
    }

    /** 相等与空：按 BEFORE 退化（「不后于」）；null 同空表。 */
    @Test
    void equalAndNullClocksDegradeToBefore() {
        assertThat(VectorClockOrder.compare(
                Map.of("n1", 1L), Map.of("n1", 1L)))
                .isEqualTo(VectorClockOrder.CausalOrder.BEFORE);
        assertThat(VectorClockOrder.compare(null, null))
                .isEqualTo(VectorClockOrder.CausalOrder.BEFORE);
    }

    /** 畸形入参 fail-fast：负分量。 */
    @Test
    void malformedClockFailsFast() {
        assertThatThrownBy(() -> VectorClockOrder.compare(
                Map.of("n1", -1L), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分量非法");
    }
}
