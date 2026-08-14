package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBusStats;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-34 / spec 13 §core-4：有界异步事件分发器——
 * 顺序交付、DropOldest 溢出丢弃计数可见、Block 入队限时、close 排空、慢交付不阻塞入队。
 */
class BufferedEventDispatcherTest {

    private static SessionEvent event(String type) {
        return SessionEvent.of(type);
    }

    @Test
    void deliversEventsInOrderOnDedicatedThread() throws Exception {
        List<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch delivered = new CountDownLatch(8);
        try (BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s1",
                EventDispatchConfig.buffered(), e -> {
                    received.add(e.type());
                    delivered.countDown();
                })) {
            for (int i = 0; i < 8; i++) {
                dispatcher.enqueue(event("e-" + i));
            }
            assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(received).containsExactly("e-0", "e-1", "e-2", "e-3", "e-4", "e-5", "e-6", "e-7");
    }

    @Test
    void dropOldestEvictsAndCountsWhenCapacityExceeded() throws Exception {
        CountDownLatch firstDeliverStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();
        EventDispatchConfig config = new EventDispatchConfig(
                EventDispatchConfig.Mode.BUFFERED, 2,
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s2",
                config, e -> {
                    if ("e0".equals(e.type())) {
                        firstDeliverStarted.countDown(); // 门闩：e0 已离开队列、在交付中
                    }
                    received.add(e.type());
                    awaitLatch(release);
                });
        dispatcher.enqueue(event("e0"));
        assertThat(firstDeliverStarted.await(5, TimeUnit.SECONDS)).isTrue();
        // 队列容量 2：e1+e2 入队占满；e3 挤掉 e1、e4 挤掉 e2 —— 丢弃 = 2、最新 e3/e4 存活
        dispatcher.enqueue(event("e1"));
        dispatcher.enqueue(event("e2"));
        dispatcher.enqueue(event("e3"));
        dispatcher.enqueue(event("e4"));
        EventBusStats stats = dispatcher.stats();
        assertThat(stats.dropped()).isEqualTo(2);
        assertThat(stats.enqueued()).isEqualTo(5);
        assertThat(stats.queueDepth()).isEqualTo(2);
        release.countDown();
        dispatcher.close();
        // e0 + 幸存的 e3/e4 交付；被挤掉的 e1/e2 不出现
        assertThat(received).containsExactly("e0", "e3", "e4");
    }

    @Test
    void blockPolicyTimesOutAndCountsWhenQueueFullAndDelivererStuck() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();
        EventDispatchConfig config = new EventDispatchConfig(
                EventDispatchConfig.Mode.BUFFERED, 1,
                EventDispatchConfig.OverflowPolicy.BLOCK, Duration.ofMillis(100));
        long pushStart = System.nanoTime();
        try (BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s3",
                config, e -> {
                    received.add(e.type());
                    awaitLatch(release);
                })) {
            dispatcher.enqueue(event("first"));      // 交付线程取走（阻塞）
            dispatcher.enqueue(event("q1"));          // 占满容量 1
            dispatcher.enqueue(event("q2"));          // BLOCK 100ms 超时 → 丢弃计数
            long blockedMillis = (System.nanoTime() - pushStart) / 1_000_000;
            assertThat(blockedMillis).isGreaterThanOrEqualTo(90); // 确实等待了限时
            EventBusStats stats = dispatcher.stats();
            assertThat(stats.dropped()).isEqualTo(1);
            release.countDown();
        }
        assertThat(received).containsExactly("first", "q1");
    }

    @Test
    void enqueueAfterCloseIsCountedAsDropped() throws Exception {
        AtomicInteger delivered = new AtomicInteger();
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s4",
                EventDispatchConfig.buffered(), e -> delivered.incrementAndGet());
        dispatcher.enqueue(event("e"));
        dispatcher.close();
        dispatcher.enqueue(event("late"));
        assertThat(dispatcher.stats().dropped()).isEqualTo(1);
    }

    @Test
    void slowDeliveryDoesNotBlockEnqueue() {
        CountDownLatch release = new CountDownLatch(1);
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s5",
                EventDispatchConfig.buffered(), e -> awaitLatch(release));
        long start = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            dispatcher.enqueue(event("e-" + i));
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        // 慢交付（阻塞在首个事件）下批量入队毫秒级完成——Turn 主链路不被拖慢（背压语义）
        assertThat(elapsedMillis).isLessThan(2_000);
        release.countDown();
        dispatcher.close();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
