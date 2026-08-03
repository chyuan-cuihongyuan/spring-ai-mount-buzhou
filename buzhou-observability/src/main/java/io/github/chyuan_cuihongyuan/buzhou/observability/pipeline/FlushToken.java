package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import java.util.concurrent.CountDownLatch;

/**
 * flush token：入队后由 drain 线程在强制批量落库后 {@link #complete()}（{@link PendingItem} 之一）。
 * {@link AsyncObservabilityPipeline#flush()} 入队后 {@link CountDownLatch#await()} 等待完成。
 */
public record FlushToken(CountDownLatch done) implements PendingItem {

    public FlushToken() {
        this(new CountDownLatch(1));
    }

    public void complete() {
        done.countDown();
    }
}
