package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5006 / T6114：票据锁合同——票据算术、乱序放行 IAE、
 * 并发互斥与全员获释、排队读数归零。
 */
class TicketLockTest {

    @Test
    void ticketArithmeticShouldAdvanceDeterministically() {
        TicketLock lock = new TicketLock();
        long first = lock.lock();
        assertThat(first).isZero();
        assertThat(lock.queueLength()).isEqualTo(1);
        lock.unlock(first);
        assertThat(lock.nowServing()).isEqualTo(1L);
        assertThat(lock.queueLength()).isZero();
        long second = lock.lock();
        assertThat(second).isEqualTo(1L);   // 下一票
        lock.unlock(second);
    }

    @Test
    void outOfOrderUnlockShouldFailFast() {
        TicketLock lock = new TicketLock();
        long ticket = lock.lock();
        assertThatThrownBy(() -> lock.unlock(ticket + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.unlock(ticket - 1))
                .isInstanceOf(IllegalArgumentException.class);
        lock.unlock(ticket);   // 正确放行不受影响
    }

    @Test
    void concurrentCounterShouldBeExactlyConsistent() throws Exception {
        TicketLock lock = new TicketLock();
        int threads = 8;
        int iterations = 200;
        AtomicInteger counter = new AtomicInteger();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);
        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < iterations; i++) {
                    long ticket = lock.lock();
                    try {
                        counter.incrementAndGet();
                    } finally {
                        lock.unlock(ticket);
                    }
                }
                done.countDown();
            }));
        }
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        for (Future<?> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
        executor.shutdownNow();
        assertThat(counter.get()).isEqualTo(threads * iterations);   // 互斥精确
        assertThat(lock.queueLength()).isZero();                     // 全员获释
        assertThat(lock.nowServing()).isEqualTo(threads * iterations);
        executor.close();
    }

    @Test
    void queueLengthShouldReflectWaitingDepth() throws Exception {
        TicketLock lock = new TicketLock();
        long held = lock.lock();
        AtomicInteger served = new AtomicInteger();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch done = new CountDownLatch(2);
        for (int t = 0; t < 2; t++) {
            executor.submit(() -> {
                long ticket = lock.lock();
                served.incrementAndGet();
                lock.unlock(ticket);
                done.countDown();
            });
        }
        // 等待者取到票（queueLength ≥ 3 = 持有 1 + 等待 2）
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (lock.queueLength() < 3 && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertThat(lock.queueLength()).isGreaterThanOrEqualTo(3);
        lock.unlock(held);
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(served.get()).isEqualTo(2);
        assertThat(lock.queueLength()).isZero();
        executor.shutdownNow();
    }
}
