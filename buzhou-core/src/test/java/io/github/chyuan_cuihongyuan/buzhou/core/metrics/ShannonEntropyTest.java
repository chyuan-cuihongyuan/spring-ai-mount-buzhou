package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2050 / T3202：香农熵合同——全集中 0、均匀 log k、零频不计、
 * 归一化 [0,1]、bits/nats 换算、畸形 fail-fast。
 */
class ShannonEntropyTest {

    @Test
    void fullyConcentratedDistributionShouldHaveZeroEntropy() {
        assertThat(ShannonEntropy.entropyBits(new long[]{100, 0, 0})).isZero();
        assertThat(ShannonEntropy.normalizedEntropy(new long[]{100})).isZero();
    }

    @Test
    void uniformDistributionShouldHitLogK() {
        // 四类均匀：H = log2(4) = 2 bits
        assertThat(ShannonEntropy.entropyBits(new long[]{25, 25, 25, 25}))
                .isCloseTo(2.0d, within(1e-12));
        assertThat(ShannonEntropy.normalizedEntropy(new long[]{25, 25, 25, 25}))
                .isCloseTo(1.0d, within(1e-12));
    }

    @Test
    void zeroCountClassesShouldNotAffectEntropy() {
        // 零频类不贡献也不抬上界——(50,50,0) 与 (50,50) 同熵
        assertThat(ShannonEntropy.entropyBits(new long[]{50, 50, 0}))
                .isCloseTo(ShannonEntropy.entropyBits(new long[]{50, 50}), within(1e-12));
        assertThat(ShannonEntropy.normalizedEntropy(new long[]{50, 50, 0}))
                .isCloseTo(1.0d, within(1e-12)); // 上界按非零类 2 计
    }

    @Test
    void knownBinaryEntropyShouldMatchHandComputation() {
        // (3,1)：H = −(3/4)log(3/4) − (1/4)log(1/4) ≈ 0.8113 bits
        assertThat(ShannonEntropy.entropyBits(new long[]{3, 1}))
                .isCloseTo(0.811278d, within(1e-5));
    }

    @Test
    void natsAndBitsShouldConvertByLogBase() {
        double bits = ShannonEntropy.entropyBits(new long[]{3, 1});
        double nats = ShannonEntropy.entropy(new long[]{3, 1}, ShannonEntropy.BASE_E);
        assertThat(nats / bits).isCloseTo(Math.log(2), within(1e-12)); // 1 bit = ln2 nats
    }

    @Test
    void normalizedEntropyShouldStayInUnitInterval() {
        // 半偏分布 (7,3)：归一化 ∈ (0,1)
        double normalized = ShannonEntropy.normalizedEntropy(new long[]{7, 3});
        assertThat(normalized).isGreaterThan(0.0d).isLessThan(1.0d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> ShannonEntropy.entropy(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShannonEntropy.entropy(new long[0], 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShannonEntropy.entropy(new long[]{1, 1}, 1))
                .isInstanceOf(IllegalArgumentException.class); // base 1 退化
        assertThatThrownBy(() -> ShannonEntropy.entropy(new long[]{0, 0}, 2))
                .isInstanceOf(IllegalArgumentException.class); // 零总量
        assertThatThrownBy(() -> ShannonEntropy.entropy(new long[]{1, -1}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShannonEntropy.normalizedEntropy(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
