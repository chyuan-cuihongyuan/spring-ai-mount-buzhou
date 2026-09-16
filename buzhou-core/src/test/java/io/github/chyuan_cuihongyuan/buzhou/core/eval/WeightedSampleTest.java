package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import io.github.chyuan_cuihongyuan.buzhou.core.eval.WeightedSample.Candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2051 / T3204：加权抽样合同——无放回不重复、零权永不中、k 截断
 * 与超取全取、确定性回放、重权多中趋势、畸形 fail-fast。
 */
class WeightedSampleTest {

    @Test
    void sampledItemsShouldBeDistinctWithoutReplacement() {
        RandomGenerator rng = new java.util.SplittableRandom(3);
        List<Candidate<String>> pool = List.of(
                new Candidate<>("a", 1), new Candidate<>("b", 1), new Candidate<>("c", 1),
                new Candidate<>("d", 1), new Candidate<>("e", 1));
        for (int trial = 0; trial < 50; trial++) {
            List<String> picked = WeightedSample.sample(pool, 3, rng);
            assertThat(picked).hasSize(3);
            assertThat(picked).doesNotHaveDuplicates(); // 无放回
        }
    }

    @Test
    void zeroWeightShouldNeverBePicked() {
        RandomGenerator rng = new java.util.SplittableRandom(5);
        List<Candidate<String>> pool = List.of(
                new Candidate<>("live", 1), new Candidate<>("dead", 0));
        for (int trial = 0; trial < 100; trial++) {
            List<String> picked = WeightedSample.sample(pool, 1, rng);
            assertThat(picked).containsExactly("live"); // 零权永不中
        }
    }

    @Test
    void kBeyondPoolShouldReturnAllEligible() {
        RandomGenerator rng = new java.util.SplittableRandom(7);
        List<Candidate<Integer>> pool = List.of(
                new Candidate<>(1, 2), new Candidate<>(2, 3), new Candidate<>(3, 0));
        List<Integer> picked = WeightedSample.sample(pool, 10, rng);
        assertThat(picked).containsExactlyInAnyOrder(1, 2); // 零权不计入
    }

    @Test
    void zeroKShouldReturnEmpty() {
        assertThat(WeightedSample.sample(List.of(new Candidate<>("a", 1)), 0,
                new java.util.SplittableRandom(1))).isEmpty();
    }

    @Test
    void sameSeedShouldReplaySamePick() {
        List<Candidate<String>> pool = List.of(
                new Candidate<>("a", 1), new Candidate<>("b", 2), new Candidate<>("c", 3),
                new Candidate<>("d", 4), new Candidate<>("e", 5));
        List<String> first = WeightedSample.sample(pool, 2, new java.util.SplittableRandom(42));
        List<String> second = WeightedSample.sample(pool, 2, new java.util.SplittableRandom(42));
        assertThat(first).isEqualTo(second); // 同种回放
    }

    @Test
    void heavyItemsShouldDominateTopOnePicks() {
        RandomGenerator rng = new java.util.SplittableRandom(11);
        List<Candidate<String>> pool = List.of(
                new Candidate<>("heavy", 100), new Candidate<>("light", 1));
        int heavyWins = 0;
        for (int trial = 0; trial < 1000; trial++) {
            if (WeightedSample.sample(pool, 1, rng).contains("heavy")) {
                heavyWins++;
            }
        }
        assertThat(heavyWins).isGreaterThan(950); // 100:1 权重下重者主导（理论 ~99%）
    }

    @Test
    void malformedInputsShouldFailFast() {
        RandomGenerator rng = new java.util.SplittableRandom(1);
        assertThatThrownBy(() -> WeightedSample.sample(null, 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedSample.sample(List.of(), 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedSample.sample(List.of(), -1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedSample.sample(
                List.of(new Candidate<>("a", -0.1)), 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightedSample.sample(
                List.of(new Candidate<>("a", Double.NaN)), 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
