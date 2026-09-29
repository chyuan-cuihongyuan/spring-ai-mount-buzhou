package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class ThompsonSamplerTest {

    @Test
    void shouldExploreEvenlyOnEmptyHistories() {
        long[] successes = {0, 0, 0};
        long[] failures = {0, 0, 0};
        Random random = new Random(8024);
        int[] counts = new int[3];
        for (int i = 0; i < 300; i++) {
            counts[ThompsonSampler.select(successes, failures, random)]++;
        }
        assertThat(counts[0]).isGreaterThan(50);
        assertThat(counts[1]).isGreaterThan(50);
        assertThat(counts[2]).isGreaterThan(50);
    }

    @Test
    void shouldConvergeToTrueBestArm() {
        long[] successes = {500, 1, 1};
        long[] failures = {5, 500, 500};
        Random random = new Random(8025);
        int bestChosen = 0;
        for (int i = 0; i < 300; i++) {
            if (ThompsonSampler.select(successes, failures, random) == 0) {
                bestChosen++;
            }
        }
        assertThat(bestChosen).isGreaterThan(240);
    }

    @Test
    void shouldBeSeedDeterministicAndFailFast() {
        long[] successes = {3, 7, 2};
        long[] failures = {9, 4, 6};
        Random first = new Random(99);
        Random second = new Random(99);
        for (int i = 0; i < 50; i++) {
            assertThat(ThompsonSampler.select(successes, failures, first))
                    .isEqualTo(ThompsonSampler.select(successes, failures, second));
        }
        assertThatThrownBy(() -> ThompsonSampler.select(null, new long[]{1}, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ThompsonSampler.select(new long[]{1}, new long[]{1, 2}, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ThompsonSampler.select(new long[]{0}, new long[]{}, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ThompsonSampler.select(new long[]{-1}, new long[]{1}, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(0.5).isCloseTo(0.5, within(0.0));
    }
}
