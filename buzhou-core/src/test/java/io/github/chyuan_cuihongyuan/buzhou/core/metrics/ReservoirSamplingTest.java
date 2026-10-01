package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11013 / Y11027：ReservoirSampling 合同验证——均匀覆盖圣像+升序
 * 去重+种子双面+fail-fast。
 */
class ReservoirSamplingTest {

    @Test
    void shouldCoverUniformly_whenManyTrials() {
        int n = 100;
        int k = 10;
        int trials = 1000;
        int[] hits = new int[n];
        for (int trial = 0; trial < trials; trial++) {
            for (int index : ReservoirSampling.sample(n, k, 1000L + trial)) {
                hits[index]++;
            }
        }
        double expectedRate = (double) trials * k / n;
        for (int element = 0; element < n; element++) {
            assertThat((double) hits[element]).as("元素 %d 入样率", element)
                    .isCloseTo(expectedRate, within(expectedRate * 0.35));
        }
    }

    @Test
    void shouldBeAscendingDistinct_whenSampled() {
        int[] sample = ReservoirSampling.sample(1000, 20, 42L);
        assertThat(sample).hasSize(20);
        assertThat(sample).isSorted();
        assertThat(sample).doesNotHaveDuplicates();
        for (int value : sample) {
            assertThat(value).isBetween(0, 999);
        }
    }

    @Test
    void shouldReturnWholePopulation_whenKEqualsN() {
        int[] sample = ReservoirSampling.sample(50, 50, 7L);
        assertThat(sample).hasSize(50);
        for (int i = 0; i < 50; i++) {
            assertThat(sample[i]).isEqualTo(i);
        }
    }

    @Test
    void shouldBeDeterministic_whenSameSeedAndDifferent_whenOtherSeed() {
        int[] first = ReservoirSampling.sample(1000, 30, 42L);
        int[] second = ReservoirSampling.sample(1000, 30, 42L);
        assertThat(second).containsExactly(first);
        int[] third = ReservoirSampling.sample(1000, 30, 43L);
        assertThat(third).isNotEqualTo(first);
    }

    @Test
    void shouldFailFast_whenInvalidSizes() {
        assertThatThrownBy(() -> ReservoirSampling.sample(100, 0, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≥1");
        assertThatThrownBy(() -> ReservoirSampling.sample(10, 11, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k≤n");
    }
}
