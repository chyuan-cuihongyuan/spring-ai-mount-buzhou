package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 磁盘 spill 写路径并发语义测试（spec 1608 / T2367–T2368 / impl 1161）：
 * monitor→ReentrantLock 迁移后「一次调用一次 spill」互斥不变——同 uri 并发
 * store 恰好一个成功、其余得 IllegalStateException；异 uri 并发写全部成功。
 */
class DiskSpillStoreConcurrencyTest {

    private static SpillEntry entryOf(String toolCallId, int seq) {
        return new SpillEntry(SpillUri.parse("spill://agent/s1/" + toolCallId),
                "内容-" + seq, "text/plain", 10, Instant.now());
    }

    @Test
    void sameUriConcurrentStoreAllowsExactlyOne(@TempDir Path dir) throws Exception {
        DiskSpillStore store = new DiskSpillStore(dir);
        int threads = 6;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<String> rejections = new CopyOnWriteArrayList<>();
        List<Thread> vts = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread vt = Thread.ofVirtual().unstarted(() -> {
                try {
                    start.await();
                    store.store(entryOf("same", 0), 20);
                    successes.incrementAndGet();
                } catch (IllegalStateException dup) {
                    rejections.add(dup.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            vts.add(vt);
            vt.start();
        }
        start.countDown();
        for (Thread vt : vts) {
            vt.join(5000);
        }
        assertThat(successes.get()).isEqualTo(1);
        assertThat(rejections).hasSize(threads - 1)
                .allSatisfy(m -> assertThat(m).contains("Spill already exists"));
    }

    @Test
    void distinctUriConcurrentStoreAllSucceed(@TempDir Path dir) throws Exception {
        DiskSpillStore store = new DiskSpillStore(dir);
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Thread> vts = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int seq = i;
            Thread vt = Thread.ofVirtual().unstarted(() -> {
                try {
                    start.await();
                    store.store(entryOf("call-" + seq, seq), 20);
                    successes.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    // 失败在断言层显形
                }
            });
            vts.add(vt);
            vt.start();
        }
        start.countDown();
        for (Thread vt : vts) {
            vt.join(5000);
        }
        assertThat(successes.get()).isEqualTo(threads);
        assertThat(store.usage().entryCount()).isEqualTo(threads);
    }
}
