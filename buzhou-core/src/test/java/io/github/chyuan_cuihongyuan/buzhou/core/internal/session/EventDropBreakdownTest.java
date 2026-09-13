package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDropBreakdown;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-653 / spec 900：事件丢弃按原因分类读面——DROP_OLDEST 分桶、
 * close 滞留分桶、守恒不变量（ΣbyReason == dropped）、快照不可变、EMPTY 常量语义。
 */
class EventDropBreakdownTest {

    private static SessionEvent event(String type) {
        return SessionEvent.of(type);
    }

    private static void awaitCount(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void dropOldestReasonIsBucketedAndConservesTotal() throws Exception {
        CountDownLatch firstDeliverStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        EventDispatchConfig config = new EventDispatchConfig(
                EventDispatchConfig.Mode.BUFFERED, 2,
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s-drop",
                config, e -> {
                    if ("e0".equals(e.type())) {
                        firstDeliverStarted.countDown();
                    }
                    awaitCount(release);
                });
        try {
            dispatcher.enqueue(event("e0"));
            awaitCount(firstDeliverStarted);
            dispatcher.enqueue(event("e1"));
            dispatcher.enqueue(event("e2"));
            dispatcher.enqueue(event("e3")); // 挤掉 e1
            dispatcher.enqueue(event("e4")); // 挤掉 e2

            EventDropBreakdown breakdown = dispatcher.dropBreakdown();
            assertThat(breakdown.forReason("drop-oldest")).isEqualTo(2);
            assertThat(breakdown.total()).isEqualTo(dispatcher.stats().dropped());
            assertThat(breakdown.byReason()).containsOnlyKeys("drop-oldest");
            // 未发生的原因读 0（不抛）
            assertThat(breakdown.forReason("block-timeout")).isZero();
        } finally {
            release.countDown();
            dispatcher.close();
        }
    }

    @Test
    void closedUndeliveredReasonIsBucketedAndConservesTotal() {
        CountDownLatch release = new CountDownLatch(1);
        EventDispatchConfig config = new EventDispatchConfig(
                EventDispatchConfig.Mode.BUFFERED, 4,
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s-close",
                config, e -> awaitCount(release));
        try {
            // 首事件堵住交付线程，后续 4 条滞留队列
            dispatcher.enqueue(event("head"));
            dispatcher.enqueue(event("a"));
            dispatcher.enqueue(event("b"));
            dispatcher.enqueue(event("c"));
            dispatcher.enqueue(event("d"));
            release.countDown();
            dispatcher.close();
            // close 后滞留者全部计 closed-undelivered（head 可能已交付——分桶数随调度，
            // 但守恒不变量必须恒成立）
            EventDropBreakdown breakdown = dispatcher.dropBreakdown();
            assertThat(breakdown.total()).isEqualTo(dispatcher.stats().dropped());
            assertThat(breakdown.forReason("closed-undelivered")
                    + breakdown.forReason("drop-oldest"))
                    .isEqualTo(breakdown.total());
        } finally {
            release.countDown();
            dispatcher.close();
        }
    }

    @Test
    void snapshotIsImmutableAndDefensivelyCopied() {
        EventDropBreakdown breakdown = new EventDropBreakdown(Map.of("drop-oldest", 2L));
        assertThatThrownBy(() -> breakdown.byReason().put("x", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyConstantHasZeroTotals() {
        assertThat(EventDropBreakdown.EMPTY.total()).isZero();
        assertThat(EventDropBreakdown.EMPTY.byReason()).isEmpty();
        assertThat(EventDropBreakdown.EMPTY.forReason("drop-oldest")).isZero();
    }
}
