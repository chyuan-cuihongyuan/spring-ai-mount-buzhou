package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3031 / T5064：vruntime 合同——等权轮转、三倍权三倍配额、
 * 空队列 null、账面按权重折算（work10/w2 → +5）、并列先入先选、
 * 随机工作量等权均衡、参数校验。
 */
class VirtualRuntimeQueueTest {

    @Test
    void equalWeightsShouldRotateRoundRobin() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("a", 1);
        queue.register("b", 1);
        queue.register("c", 1);
        StringBuilder picks = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            picks.append(queue.pickNext(1));
        }
        assertThat(picks.toString()).isEqualTo("abcabcabc");
    }

    @Test
    void tripleWeightShouldEarnTripleQuota() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("heavy", 3);
        queue.register("light", 1);
        long heavy = 0;
        long light = 0;
        for (int i = 0; i < 1_000; i++) {
            if ("heavy".equals(queue.pickNext(1))) {
                heavy++;
            } else {
                light++;
            }
        }
        assertThat(heavy).isBetween(690L, 810L);   // 期望 750 ±
        assertThat(light).isBetween(190L, 310L);   // 期望 250 ±
    }

    @Test
    void emptyQueueShouldReturnNull() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        assertThat(queue.pickNext(1)).isNull();
        assertThat(queue.size()).isZero();
    }

    @Test
    void vruntimeShouldAdvanceScaledByWeight() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("x", 2);
        queue.pickNext(10);
        assertThat(queue.vruntimeOf("x")).isEqualTo(5.0);  // work10/weight2
        queue.pickNext(10);
        assertThat(queue.vruntimeOf("x")).isEqualTo(10.0);
    }

    @Test
    void vruntimeTieShouldPickEarliestRegistered() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("first", 1);
        queue.register("second", 1);
        assertThat(queue.pickNext(0)).isEqualTo("first");   // work 0 纯重选
        assertThat(queue.pickNext(0)).isEqualTo("second");  // 重插序仍其后
    }

    @Test
    void randomWorkloadShouldEqualizeEqualWeights() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(17);
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("p", 1);
        queue.register("q", 1);
        long p = 0;
        long q = 0;
        for (int i = 0; i < 2_000; i++) {
            double work = rng.nextInt(1, 10);
            if ("p".equals(queue.pickNext(work))) {
                p++;
            } else {
                q++;
            }
        }
        assertThat(p / (double) (p + q)).isCloseTo(0.5, within(0.05));
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        VirtualRuntimeQueue queue = new VirtualRuntimeQueue();
        queue.register("a", 1);
        assertThatThrownBy(() -> queue.register("a", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.register("b", 0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.register(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.pickNext(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(queue.vruntimeOf("ghost")).isNaN();
        assertThat(queue.contains("a")).isTrue();
    }
}
