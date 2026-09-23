package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5007 / T6116：SeqLock 合同——奇偶算术、跨写校验失败、
 * 并发二元组不撕裂、畸形 writeEnd IAE。
 */
class SeqLockTest {

    @Test
    void sequenceParityShouldMarkWritePhase() {
        SeqLock lock = new SeqLock();
        assertThat(lock.sequence()).isZero();
        long writeSeq = lock.writeBegin();
        assertThat(writeSeq).isEqualTo(1L);   // 奇=写入中
        assertThat(lock.isWriting()).isTrue();
        lock.writeEnd(writeSeq);
        assertThat(lock.sequence()).isEqualTo(2L);   // 偶=稳定
        assertThat(lock.isWriting()).isFalse();
    }

    @Test
    void validationShouldFailAcrossConcurrentWrite() {
        SeqLock lock = new SeqLock();
        long observed = lock.readBegin();   // 稳定期拍照
        long writeSeq = lock.writeBegin();  // 写者介入
        lock.writeEnd(writeSeq);
        assertThat(lock.readEnd(observed)).isFalse();   // 拍照过期——整读作废
        long fresh = lock.readBegin();
        assertThat(lock.readEnd(fresh)).isTrue();       // 新拍照一致
    }

    @Test
    void readersShouldNeverObserveTornPairs() throws Exception {
        SeqLock lock = new SeqLock();
        AtomicReference<String> shared = new AtomicReference<>("v0");
        AtomicLong version = new AtomicLong(0);
        int writes = 500;
        int readers = 4;
        AtomicInteger tornDetected = new AtomicInteger();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch done = new CountDownLatch(readers);
        for (int r = 0; r < readers; r++) {
            executor.submit(() -> {
                try {
                    while (version.get() < writes) {
                        long seq = lock.readBegin();
                        if (seq % 2 == 1) {
                            continue;   // 写入中——重试
                        }
                        String value = shared.get();   // 读共享对
                        if (!lock.readEnd(seq)) {
                            continue;   // 过期——重试
                        }
                        if (!value.equals("v" + version.get())) {
                            tornDetected.incrementAndGet();   // 撕裂即违合同
                        }
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        for (int i = 1; i <= writes; i++) {
            long writeSeq = lock.writeBegin();
            shared.set("v" + i);
            version.set(i);
            lock.writeEnd(writeSeq);
        }
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(tornDetected.get()).isZero();   // 读者观察到的对永不撕裂
        assertThat(shared.get()).isEqualTo("v" + writes);
    }

    @Test
    void invalidWriteEndShouldFailFast() {
        SeqLock lock = new SeqLock();
        assertThatThrownBy(() -> lock.writeEnd(0L))   // 偶序号非写入期
                .isInstanceOf(IllegalArgumentException.class);
        long writeSeq = lock.writeBegin();
        lock.writeEnd(writeSeq);
        assertThatThrownBy(() -> lock.writeEnd(writeSeq))   // 重复结束（已被推进）
                .isInstanceOf(IllegalArgumentException.class);
    }
}
