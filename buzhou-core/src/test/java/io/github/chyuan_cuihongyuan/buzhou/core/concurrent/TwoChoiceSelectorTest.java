package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3022 / T5046：二择合同——轻者占优（重载桶仅两抽同中才中）、
 * 二择最大负载 ≤ 单抽（万球十桶对拍）、等负载均匀、单桶恒 0、
 * 参数 fail-fast、同种子回放。
 */
class TwoChoiceSelectorTest {

    private static final RandomGeneratorFactory<RandomGenerator> RNG_FACTORY =
            RandomGeneratorFactory.of("L64X256MixRandom");

    @Test
    void lighterBinShouldDominateChoices() {
        // 唯一空桶仅当两抽都错过（概率 (3/4)×(2/3)=1/2）才不中——
        // 选中率 ≈1/2，是单抽均匀率 1/4 的 2×（轻者占优的最小可断言）
        int[] loads = {100, 100, 100, 0};
        RandomGenerator rng = RNG_FACTORY.create(42);
        int emptyPicks = 0;
        int draws = 10_000;
        for (int i = 0; i < draws; i++) {
            if (TwoChoiceSelector.pick(loads, rng) == 3) {
                emptyPicks++;
            }
        }
        assertThat(emptyPicks / (double) draws).isBetween(0.47, 0.53);
    }

    @Test
    void twoChoiceMaxLoadShouldBeatSingleRandom() {
        // 万球十桶：二择最大负载 ≤ 单抽最大负载（两策略同种子族）
        int bins = 10;
        int balls = 10_000;
        int[] twoChoiceLoads = new int[bins];
        int[] singleLoads = new int[bins];
        RandomGenerator tc = RNG_FACTORY.create(7);
        RandomGenerator sr = RNG_FACTORY.create(7);
        for (int i = 0; i < balls; i++) {
            twoChoiceLoads[TwoChoiceSelector.pick(twoChoiceLoads, tc)]++;
            int single = sr.nextInt(bins);
            singleLoads[single]++;
        }
        int tcMax = java.util.Arrays.stream(twoChoiceLoads).max().orElseThrow();
        int srMax = java.util.Arrays.stream(singleLoads).max().orElseThrow();
        assertThat(tcMax).isLessThanOrEqualTo(srMax);
        // 二择紧界：均值 1000 + 少量溢出（Θ(ln ln n) 量级）
        assertThat(tcMax).isLessThanOrEqualTo(1_020);
    }

    @Test
    void equalLoadsShouldFallBackToUniform() {
        int[] loads = new int[10];
        RandomGenerator rng = RNG_FACTORY.create(11);
        long[] counts = new long[10];
        for (int i = 0; i < 10_000; i++) {
            counts[TwoChoiceSelector.pick(loads, rng)]++;
        }
        for (int b = 0; b < 10; b++) {
            assertThat(counts[b]).isBetween(800L, 1_200L);
        }
    }

    @Test
    void singleBinShouldAlwaysWin() {
        RandomGenerator rng = RNG_FACTORY.create(3);
        assertThat(TwoChoiceSelector.pick(new int[] {5}, rng)).isZero();
        assertThat(TwoChoiceSelector.pick(new int[] {0}, rng)).isZero();
    }

    @Test
    void sameSeedShouldReplay() {
        int[] loads = {4, 2, 9, 2};
        RandomGenerator a = RNG_FACTORY.create(99);
        RandomGenerator b = RNG_FACTORY.create(99);
        for (int i = 0; i < 100; i++) {
            assertThat(TwoChoiceSelector.pick(loads, a))
                    .isEqualTo(TwoChoiceSelector.pick(loads, b));
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        RandomGenerator rng = RNG_FACTORY.create(1);
        assertThatThrownBy(() -> TwoChoiceSelector.pick(new int[0], rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TwoChoiceSelector.pick(null, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TwoChoiceSelector.pick(new int[] {1, -2}, rng))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
