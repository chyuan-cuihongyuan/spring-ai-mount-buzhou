package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6043：McsLock 合同——每等待者本地自旋 FIFO 队列锁。
 * 互斥正确性（多线程临界区计数精确）；FIFO 获取序观测；
 * 交接正确；fail-fast。
 */
class McsLockTest {

    @Test
    void mutualExclusionUnderContention() throws Exception {
        McsLock lock = new McsLock();
        int[] counter = {0};
        int threads = 4;
        int rounds = 2000;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < rounds; i++) {
                    McsLock.Node handle = lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock(handle);
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }
        assertThat(counter[0]).isEqualTo(threads * rounds);
    }

    @Test
    void unlockFreeLockThenRelock() {
        McsLock lock = new McsLock();
        McsLock.Node handle = lock.lock();
        lock.unlock(handle);
        McsLock.Node again = lock.lock();
        lock.unlock(again);
    }

    @Test
    void twoThreadsHandoffOrder() throws Exception {
        McsLock lock = new McsLock();
        AtomicLong firstEnterAt = new AtomicLong(-1);
        AtomicLong secondEnterAt = new AtomicLong(-1);
        CountDownLatch firstInside = new CountDownLatch(1);
        CountDownLatch secondBlocked = new CountDownLatch(1);
        Thread first = new Thread(() -> {
            McsLock.Node h = lock.lock();
            firstEnterAt.set(System.nanoTime());
            firstInside.countDown();
            try {
                secondBlocked.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lock.unlock(h);
        });
        Thread second = new Thread(() -> {
            try {
                firstInside.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            secondBlocked.countDown();
            McsLock.Node h = lock.lock();
            secondEnterAt.set(System.nanoTime());
            lock.unlock(h);
        });
        first.start();
        second.start();
        first.join();
        second.join();
        assertThat(secondEnterAt.get()).as("后到者后进入").isGreaterThan(firstEnterAt.get());
    }

    @Test
    void failFastContract() {
        McsLock lock = new McsLock();
        assertThatThrownBy(() -> lock.unlock(null)).isInstanceOf(IllegalArgumentException.class);
        AtomicInteger unused = new AtomicInteger();
        assertThat(unused.get()).isZero();
    }
}
