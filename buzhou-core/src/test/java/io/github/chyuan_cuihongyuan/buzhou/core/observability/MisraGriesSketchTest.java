package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1848 / T2898：Misra-Gries——多数项幸存、计数下界、确定性。 */
class MisraGriesSketchTest {

    /** 多数项（>N/2）必幸存：k=2 一轮抵消后仍余。 */
    @Test
    void majorityElementSurvives() {
        // 6 个 a + 3 个 b（穿插）：a 占 2/3 > 1/2，k=2 必幸存
        List<String> stream = List.of("a", "b", "a", "b", "a", "b", "a", "a", "a");
        Map<String, Long> sketch = MisraGriesSketch.sketch(2, stream);
        assertThat(MisraGriesSketch.isHeavyCandidate("a", sketch)).isTrue();
        // 下界保证：真实 6 − 估计 ≤ N/k = 9/2 = 4.5 → 估计 ≥ 2（且 ≤ 6）
        assertThat(sketch.get("a")).isBetween(2L, 6L);
    }

    /** 计数下界口径：估计 ≤ 真实 且 真实−估计 ≤ N/k。 */
    @Test
    void countIsUnderestimateWithinBound() {
        // 10 个 a + 10 个杂项，k=3（阈 1/3，a 占 1/2 是频项）
        List<String> stream = new java.util.ArrayList<>();
        IntStream.range(0, 10).forEach(i -> {
            stream.add("a");
            stream.add("noise-" + i);
        });
        Map<String, Long> sketch = MisraGriesSketch.sketch(3, stream);
        Long approx = sketch.get("a");
        if (approx != null) {
            assertThat(approx).isLessThanOrEqualTo(10L);
            assertThat(10L - approx).isLessThanOrEqualTo(20L / 3);
        } else {
            // 未幸存则违背频项保证——a 占 1/2 > 1/3 必须幸存
            assertThat(MisraGriesSketch.isHeavyCandidate("a", sketch)).isTrue();
        }
    }

    /** 确定性与空流：同流同素描；null/空 → 空。 */
    @Test
    void deterministicAndEmptyBehave() {
        List<String> stream = List.of("x", "y", "x", "z", "x");
        assertThat(MisraGriesSketch.sketch(3, stream))
                .isEqualTo(MisraGriesSketch.sketch(3, stream));
        assertThat(MisraGriesSketch.sketch(3, List.of())).isEmpty();
        assertThat(MisraGriesSketch.sketch(3, null)).isEmpty();
        assertThat(MisraGriesSketch.isHeavyCandidate("x", Map.of())).isFalse();
    }

    /** 畸形入参 fail-fast：k < 2、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> MisraGriesSketch.sketch(1, List.of("a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k 不能小于 2");
        assertThatThrownBy(() -> MisraGriesSketch.sketch(2,
                java.util.Arrays.asList("a", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流元素不能为 null");
    }
}
