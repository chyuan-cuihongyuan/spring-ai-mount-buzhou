package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3016 / T5034：攒批合同——条数满即冲（龄未到）、龄到即冲
 * （数未满）、双未达不冲、冲后批龄重锚、守恒（offered==flushed+
 * 在批）、空批 drain 不动账、三批循环账面、配置校验、随机操作
 * 守恒恒等。
 */
class BatchAccumulatorTest {

    @Test
    void sizeThresholdShouldFlushEvenWhenYoung() {
        BatchAccumulator<String> acc = new BatchAccumulator<>(3, 10_000);
        assertThat(acc.offer("a", 0)).isFalse();
        assertThat(acc.offer("b", 0)).isFalse();
        assertThat(acc.offer("c", 1)).isTrue();  // 条数满——龄仅 1ms
        assertThat(acc.flushReady(1)).isTrue();
        assertThat(acc.drain()).containsExactly("a", "b", "c");
    }

    @Test
    void lingerThresholdShouldFlushEvenWhenNotFull() {
        BatchAccumulator<String> acc = new BatchAccumulator<>(1_000, 100);
        acc.offer("solo", 0);
        assertThat(acc.flushReady(99)).isFalse();
        assertThat(acc.flushReady(100)).isTrue();  // 龄恰到
        assertThat(acc.drain()).containsExactly("solo");
    }

    @Test
    void neitherThresholdShouldHold() {
        BatchAccumulator<String> acc = new BatchAccumulator<>(10, 100);
        acc.offer("x", 0);
        assertThat(acc.flushReady(50)).isFalse();
        assertThat(acc.size()).isEqualTo(1);
        assertThat(acc.isEmpty()).isFalse();
    }

    @Test
    void drainShouldReanchorBatchAge() {
        BatchAccumulator<String> acc = new BatchAccumulator<>(10, 100);
        acc.offer("first", 0);
        acc.drain();
        assertThat(acc.oldestAge(1_000)).isZero();
        acc.offer("second", 2_000);
        assertThat(acc.oldestAge(2_050)).isEqualTo(50);
        assertThat(acc.flushReady(2_099)).isFalse();
        assertThat(acc.flushReady(2_100)).isTrue();
    }

    @Test
    void conservationShouldHoldUnderRandomOps() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(23);
        BatchAccumulator<Integer> acc = new BatchAccumulator<>(4, 50);
        long now = 0;
        for (int op = 0; op < 2_000; op++) {
            now += rng.nextInt(10);
            if (rng.nextBoolean()) {
                acc.offer(op, now);
            } else if (acc.flushReady(now)) {
                acc.drain();
            }
            assertThat(acc.totalOffered()).as("op %d 守恒", op)
                    .isEqualTo(acc.totalFlushed() + acc.size());
        }
    }

    @Test
    void emptyDrainShouldNotTouchLedger() {
        BatchAccumulator<String> acc = new BatchAccumulator<>(2, 10);
        assertThat(acc.drain()).isEmpty();
        assertThat(acc.flushCount()).isZero();
        assertThat(acc.totalFlushed()).isZero();
        assertThat(acc.isEmpty()).isTrue();
    }

    @Test
    void multipleBatchesShouldAccount() {
        BatchAccumulator<Integer> acc = new BatchAccumulator<>(2, 1_000);
        acc.offer(1, 0);
        acc.offer(2, 0);
        acc.drain();
        acc.offer(3, 5);
        acc.offer(4, 5);
        acc.drain();
        acc.offer(5, 9);  // 龄到冲
        assertThat(acc.flushReady(1_009 + 1_000)).isTrue();
        acc.drain();
        assertThat(acc.flushCount()).isEqualTo(3);
        assertThat(acc.totalFlushed()).isEqualTo(5);
        assertThat(acc.totalOffered()).isEqualTo(5);
    }

    @Test
    void invalidConfigShouldFailFast() {
        assertThatThrownBy(() -> new BatchAccumulator<String>(0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BatchAccumulator<String>(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BatchAccumulator<String>(-1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void drainedBatchesShouldPreserveOfferOrder() {
        BatchAccumulator<Integer> acc = new BatchAccumulator<>(5, 1_000);
        for (int i = 0; i < 5; i++) {
            acc.offer(i, 0);
        }
        List<Integer> batch = acc.drain();
        assertThat(batch).containsExactly(0, 1, 2, 3, 4);
    }
}
