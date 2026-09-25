package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6039：WeightedReservoirSampler 合同——A-Chao 加权
 * 蓄水池。池容量守恒；重权项高概率入样；同种子同样本；
 * fail-fast。
 */
class WeightedReservoirSamplerTest {

    @Test
    void poolSizeNeverExceedsCapacity() {
        WeightedReservoirSampler sampler = new WeightedReservoirSampler(10, 6039L);
        for (long item = 1; item <= 500; item++) {
            sampler.offer(item, 1 + item % 5);
            assertThat(sampler.size()).isLessThanOrEqualTo(10);
        }
        assertThat(sampler.size()).isEqualTo(10);
        assertThat(sampler.seenCount()).isEqualTo(500);
    }

    @Test
    void heavyItemDominatesSample() {
        WeightedReservoirSampler sampler = new WeightedReservoirSampler(5, 77L);
        sampler.offer(999, 1_000_000);
        for (long light = 1; light <= 200; light++) {
            sampler.offer(light, 1);
        }
        assertThat(sampler.sample()).as("百万权重项应驻留池中").contains(999L);
    }

    @Test
    void sameSeedSameSample() {
        WeightedReservoirSampler a = new WeightedReservoirSampler(8, 12345L);
        WeightedReservoirSampler b = new WeightedReservoirSampler(8, 12345L);
        for (long item = 1; item <= 100; item++) {
            a.offer(item, 1 + item % 3);
            b.offer(item, 1 + item % 3);
        }
        assertThat(b.sample()).containsExactlyElementsOf(a.sample());
    }

    @Test
    void underCapacityKeepsEverything() {
        WeightedReservoirSampler sampler = new WeightedReservoirSampler(10, 1L);
        for (long item = 1; item <= 7; item++) {
            sampler.offer(item, 2);
        }
        assertThat(sampler.size()).isEqualTo(7);
        for (long item = 1; item <= 7; item++) {
            assertThat(sampler.sample()).contains(item);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new WeightedReservoirSampler(0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        WeightedReservoirSampler sampler = new WeightedReservoirSampler(5, 1L);
        assertThatThrownBy(() -> sampler.offer(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.offer(1, -2)).isInstanceOf(IllegalArgumentException.class);
    }
}
