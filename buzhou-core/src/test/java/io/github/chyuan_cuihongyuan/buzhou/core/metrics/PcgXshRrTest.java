package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * PcgXshRr 契约测试（spec 10027 / X10056）：确定性复现 + 均匀性
 * 卡方上界 + 流分离 + 值域 + 双精度域。
 */
class PcgXshRrTest {

    @Test
    void shouldBeDeterministicForSameSeedAndStream() {
        PcgXshRr first = new PcgXshRr(42L, 54L);
        PcgXshRr second = new PcgXshRr(42L, 54L);
        for (int i = 0; i < 100; i++) {
            assertThat(first.nextInt()).isEqualTo(second.nextInt());
        }
    }

    @Test
    void shouldPassChiSquareUniformity() {
        PcgXshRr generator = new PcgXshRr(20260930L, 1L);
        int buckets = 16;
        int draws = 160_000;
        int[] counts = new int[buckets];
        for (int i = 0; i < draws; i++) {
            counts[(int) (generator.nextInt() >>> 28)]++;
        }
        double expected = (double) draws / buckets;
        double chiSquare = 0.0;
        for (int count : counts) {
            double delta = count - expected;
            chiSquare += delta * delta / expected;
        }
        assertThat(chiSquare).as("15 自由度卡方").isLessThan(45.0);
    }

    @Test
    void shouldSeparateStreams() {
        PcgXshRr streamA = new PcgXshRr(7L, 1L);
        PcgXshRr streamB = new PcgXshRr(7L, 2L);
        int differing = 0;
        for (int i = 0; i < 32; i++) {
            if (streamA.nextInt() != streamB.nextInt()) {
                differing++;
            }
        }
        assertThat(differing).as("异流序列分歧").isGreaterThan(24);
    }

    @Test
    void shouldProduceDoublesInRangeAndLongs() {
        PcgXshRr generator = new PcgXshRr(1L, 3L);
        for (int i = 0; i < 1000; i++) {
            double v = generator.nextDouble();
            assertThat(v).isBetween(0.0, 1.0 - 1e-9);
        }
        long value = generator.nextLong();
        assertThat(value).isNotZero();
    }
}
