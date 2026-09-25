package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6024：MpscQueue 合同——多生产者单消费者有界 FIFO。
 * 单线程 FIFO；满拒新；多生产者不丢不重且单生产者内保序；
 * fail-fast。
 */
class MpscQueueTest {

    @Test
    void singleThreadFifoAndFullRejection() {
        MpscQueue<Integer> queue = new MpscQueue<>(3);
        assertThat(queue.offer(1)).isTrue();
        assertThat(queue.offer(2)).isTrue();
        assertThat(queue.offer(3)).isTrue();
        assertThat(queue.offer(4)).as("满拒新").isFalse();
        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.poll()).isEqualTo(1);
        assertThat(queue.poll()).isEqualTo(2);
        assertThat(queue.poll()).isEqualTo(3);
        assertThat(queue.poll()).isNull();
        assertThat(queue.isEmpty()).isTrue();
    }

    @Test
    void multiProducerNoLossAndPerProducerOrder() throws Exception {
        MpscQueue<long[]> queue = new MpscQueue<>(256);
        int producers = 4;
        int perProducer = 2000;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < producers; p++) {
            final int pid = p;
            Thread t = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < perProducer; i++) {
                    while (!queue.offer(new long[]{pid, i})) {
                        Thread.yield();
                    }
                }
            });
            threads.add(t);
            t.start();
        }
        start.countDown();
        List<long[]> consumed = new ArrayList<>();
        int expected = producers * perProducer;
        while (consumed.size() < expected) {
            long[] item = queue.poll();
            if (item != null) {
                consumed.add(item);
            } else {
                Thread.yield();
            }
        }
        for (Thread t : threads) {
            t.join();
        }
        assertThat(consumed).hasSize(expected);
        for (int p = 0; p < producers; p++) {
            List<Long> sequence = new ArrayList<>();
            for (long[] item : consumed) {
                if (item[0] == p) {
                    sequence.add(item[1]);
                }
            }
            assertThat(sequence).as("生产者 %d 单产内保序", p).isSorted();
        }
    }

    @Test
    void capacityBoundRespectedUnderContention() throws Exception {
        MpscQueue<Integer> queue = new MpscQueue<>(8);
        AtomicInteger accepted = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> producers = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < 500; i++) {
                    if (queue.offer(i)) {
                        accepted.incrementAndGet();
                    }
                }
            });
            producers.add(t);
            t.start();
        }
        start.countDown();
        for (Thread t : producers) {
            t.join();
        }
        assertThat(queue.size()).isLessThanOrEqualTo(8);
        assertThat(accepted.get() + queue.size()).isLessThanOrEqualTo(1500);
        assertThat(accepted.get()).isGreaterThanOrEqualTo(queue.size() - 8);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new MpscQueue<>(0)).isInstanceOf(IllegalArgumentException.class);
        MpscQueue<Integer> queue = new MpscQueue<>(2);
        assertThatThrownBy(() -> queue.offer(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(queue.capacity()).isEqualTo(2);
        assertThat(queue.poll()).isNull();
    }
}
