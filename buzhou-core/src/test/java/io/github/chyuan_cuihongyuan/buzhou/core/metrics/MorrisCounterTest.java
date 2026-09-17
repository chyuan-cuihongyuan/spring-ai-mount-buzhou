package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 3008 / T5018：Morris 近似计数合同——零态诚实、首计数必中、
 * 期望跟踪真值（多种子均值）、估计单调不降、指数亚对数有界、
 * 归零、同种子回放。
 */
class MorrisCounterTest {

    private static final RandomGeneratorFactory<RandomGenerator> RNG_FACTORY =
            RandomGeneratorFactory.of("L64X256MixRandom");

    @Test
    void zeroStateShouldEstimateZero() {
        MorrisCounter counter = new MorrisCounter(RNG_FACTORY.create(1));
        assertThat(counter.estimate()).isZero();
        assertThat(counter.rawExponent()).isZero();
    }

    @Test
    void firstIncrementShouldAlwaysCount() {
        // v=0 概率 2^0=1——首计数必中（无冷启动丢失）
        MorrisCounter counter = new MorrisCounter(RNG_FACTORY.create(2));
        counter.increment();
        assertThat(counter.rawExponent()).isEqualTo(1);
        assertThat(counter.estimate()).isEqualTo(1);
    }

    @Test
    void ensembleMeanShouldTrackTrueCount() {
        // 500 件各 64 增量：E[estimate]=64（无偏），均值紧界
        long total = 0;
        int counters = 500;
        int trueCount = 64;
        for (int i = 0; i < counters; i++) {
            MorrisCounter c = new MorrisCounter(RNG_FACTORY.create(1_000 + i));
            for (int k = 0; k < trueCount; k++) {
                c.increment();
            }
            total += c.estimate();
        }
        double mean = total / (double) counters;
        assertThat(mean).isBetween(trueCount * 0.8, trueCount * 1.25);
    }

    @Test
    void estimateShouldNeverDecrease() {
        MorrisCounter counter = new MorrisCounter(RNG_FACTORY.create(42));
        long previous = counter.estimate();
        for (int i = 0; i < 2_000; i++) {
            counter.increment();
            long current = counter.estimate();
            assertThat(current).isGreaterThanOrEqualTo(previous);
            previous = current;
        }
    }

    @Test
    void exponentShouldStaySubLogarithmic() {
        // 1000 次增量指数 ≤ 15（真值 log2(1001)≈10——宽松上界，
        // 反证线性计数病的空间主张）
        MorrisCounter counter = new MorrisCounter(RNG_FACTORY.create(7));
        for (int i = 0; i < 1_000; i++) {
            counter.increment();
        }
        assertThat(counter.rawExponent()).isLessThanOrEqualTo(15);
    }

    @Test
    void resetShouldZeroTheCounter() {
        MorrisCounter counter = new MorrisCounter(RNG_FACTORY.create(9));
        for (int i = 0; i < 50; i++) {
            counter.increment();
        }
        counter.reset();
        assertThat(counter.rawExponent()).isZero();
        assertThat(counter.estimate()).isZero();
    }

    @Test
    void sameSeedShouldReplaySameTrajectory() {
        MorrisCounter a = new MorrisCounter(RNG_FACTORY.create(123));
        MorrisCounter b = new MorrisCounter(RNG_FACTORY.create(123));
        for (int i = 0; i < 100; i++) {
            a.increment();
            b.increment();
        }
        assertThat(a.rawExponent()).isEqualTo(b.rawExponent());
        assertThat(a.estimate()).isEqualTo(b.estimate());
    }
}
