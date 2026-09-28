package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6025：StripedLock 合同——键哈希到固定条带的锁池。
 * 同键恒同锁；条带互斥并发正确；withLock 两条重载；确定性
 * 混淆；fail-fast。
 */
class StripedLockTest {

    @Test
    void sameKeyAlwaysSameStripe() {
        StripedLock striped = new StripedLock(16);
        Lock a1 = striped.lockFor(42);
        Lock a2 = striped.lockFor(42);
        assertThat(a1).isSameAs(a2);
        assertThat(striped.stripeCount()).isEqualTo(16);
        assertThat(striped.indexFor(42)).isEqualTo(striped.indexFor(42));
    }

    @Test
    void stripesRoundUpToPowerOfTwo() {
        assertThat(new StripedLock(5).stripeCount()).isEqualTo(8);
        assertThat(new StripedLock(16).stripeCount()).isEqualTo(16);
        assertThat(new StripedLock(1).stripeCount()).isEqualTo(1);
    }

    @Test
    void mutualExclusionOnSameKeyUnderContention() throws Exception {
        StripedLock striped = new StripedLock(8);
        long sharedKey = 77L;
        int[] counter = {0};
        int threads = 4;
        int increments = 5000;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < increments; i++) {
                    striped.withLock(sharedKey, () -> counter[0]++);
                }
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }
        assertThat(counter[0]).isEqualTo(threads * increments);
    }

    @Test
    void differentKeysRunConcurrently() throws Exception {
        StripedLock striped = new StripedLock(8);
        java.util.concurrent.atomic.AtomicBoolean overlapped = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger inCritical = new java.util.concurrent.atomic.AtomicInteger();
        Thread t1 = new Thread(() -> striped.withLock(1L, () -> {
            inCritical.incrementAndGet();
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            inCritical.decrementAndGet();
        }));
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            striped.withLock(2L, () -> overlapped.set(inCritical.get() > 0));
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertThat(overlapped).as("不同键不同条带可并行").isTrue();
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new StripedLock(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StripedLock(-4)).isInstanceOf(IllegalArgumentException.class);
        StripedLock striped = new StripedLock(4);
        assertThatThrownBy(() -> striped.withLock(1L, (java.util.function.Supplier<Object>) null))
                .isInstanceOf(NullPointerException.class);
    }
}
