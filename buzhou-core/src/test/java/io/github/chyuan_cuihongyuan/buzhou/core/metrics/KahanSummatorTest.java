package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 7032：KahanSummator 合同——补偿求和误差 O(1)。
 * 大数吃小数病钉住（朴素连加丢失、Kahan 保留）；计数/
 * 读数；确定性。
 */
class KahanSummatorTest {

    @Test
    void compensatedSumRescuesSmallAdditions() {
        KahanSummator kahan = new KahanSummator();
        double naive = 0;
        kahan.add(1e16);
        naive += 1e16;
        for (int i = 0; i < 5; i++) {
            kahan.add(1.0);
            naive += 1.0;
        }
        assertThat(naive).as("朴素连加被 ulp(1e16)=2 吞掉全部小量").isEqualTo(1e16);
        assertThat(kahan.value()).as("补偿求和至少救回部分小量").isGreaterThan(naive);
        assertThat(kahan.count()).isEqualTo(6);
    }

    @Test
    void mixedMagnitudesDeterministic() {
        KahanSummator a = new KahanSummator();
        KahanSummator b = new KahanSummator();
        Random rng = new Random(7032L);
        for (int i = 0; i < 1000; i++) {
            double value = rng.nextDouble() * Math.pow(10, rng.nextInt(10));
            a.add(value);
            b.add(value);
        }
        assertThat(a.value()).isEqualTo(b.value());
        assertThat(a.value()).isBetween(0.0, 1e12);
    }

    @Test
    void emptySummator() {
        KahanSummator kahan = new KahanSummator();
        assertThat(kahan.value()).isEqualTo(0.0);
        assertThat(kahan.count()).isZero();
    }
}
