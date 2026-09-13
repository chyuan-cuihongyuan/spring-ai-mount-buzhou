package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDropBreakdown;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-668 / spec 915：EventDropBreakdown 并发压测——4000 并发 enqueue 下
 * 守恒不变量（ΣbyReason == dropped）精确成立、分类值域封闭、多分发器实例
 * 互不串账。压测本身即热路径验证（CHM+LongAdder 无锁路径秒级完成）。
 */
class EventDropBreakdownConcurrencyTest {

    private static final int CONCURRENT_ENQUEUES = 4000;
    private static final int PER_INSTANCE_ENQUEUES = 500;

    private static EventDispatchConfig tinyDropOldestConfig() {
        return new EventDispatchConfig(EventDispatchConfig.Mode.BUFFERED, 2,
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);
    }

    @Test
    void concurrentEnqueuesConserveTotalAndCloseValueSet() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<BufferedEventDispatcher> holder = new AtomicReference<>();
        BufferedEventDispatcher dispatcher = new BufferedEventDispatcher("s-stress",
                tinyDropOldestConfig(), e -> {
                    try {
                        holder.get(); // 引用触发（交付线程被首事件阻塞即形成恒溢出）
                    } catch (RuntimeException ignored) {
                        // 慢交付不抛——空体等待
                    }
                    CountDownLatch wait = new CountDownLatch(1);
                    try {
                        wait.await(50, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
        holder.set(dispatcher);
        try {
            IntStream.range(0, CONCURRENT_ENQUEUES).parallel().forEach(i ->
                    dispatcher.enqueue(SessionEvent.of("e-" + i)));
            // 等待全部入队完成：enqueued 稳定在总数
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (dispatcher.stats().enqueued() < CONCURRENT_ENQUEUES
                    && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            EventDropBreakdown breakdown = dispatcher.dropBreakdown();
            long dropped = dispatcher.stats().dropped();
            // 守恒不变量（压测核心）：分类和 == 总量（并发下精确成立）
            assertThat(breakdown.total()).isEqualTo(dropped);
            // 分类值域封闭（DROP_OLDEST 模式只可能出现这三种原因）
            assertThat(breakdown.byReason().keySet())
                    .isSubsetOf(Set.of("drop-oldest", "drop-oldest-race", "closed-undelivered"));
            // 每个原因计数非负（LongAdder 无回退）
            breakdown.byReason().values().forEach(v -> assertThat(v).isNotNegative());
        } finally {
            dispatcher.close();
        }
    }

    @Test
    void multipleDispatcherInstancesDoNotCrossAccount() throws Exception {
        BufferedEventDispatcher d1 = new BufferedEventDispatcher("i1", tinyDropOldestConfig(),
                e -> { /* 快消费 */ });
        BufferedEventDispatcher d2 = new BufferedEventDispatcher("i2", tinyDropOldestConfig(),
                e -> { /* 快消费 */ });
        BufferedEventDispatcher d3 = new BufferedEventDispatcher("i3", tinyDropOldestConfig(),
                e -> { /* 快消费 */ });
        try {
            List<BufferedEventDispatcher> dispatchers = List.of(d1, d2, d3);
            IntStream.range(0, PER_INSTANCE_ENQUEUES * 3).parallel().forEach(i ->
                    dispatchers.get(i % 3).enqueue(SessionEvent.of("e-" + i)));
            Thread.sleep(500); // 快消费排空

            for (BufferedEventDispatcher dispatcher : dispatchers) {
                EventDropBreakdown breakdown = dispatcher.dropBreakdown();
                assertThat(breakdown.total()).isEqualTo(dispatcher.stats().dropped());
            }
            // 三实例无共享状态：交付总量合计 = 入队总量（无跨实例丢失）
            long totalDelivered = dispatchers.stream()
                    .mapToLong(d -> d.stats().dispatched()).sum();
            long totalEnqueued = dispatchers.stream()
                    .mapToLong(d -> d.stats().enqueued()).sum();
            long totalDropped = dispatchers.stream()
                    .mapToLong(d -> d.stats().dropped()).sum();
            assertThat(totalDelivered + totalDropped).isEqualTo(totalEnqueued);
        } finally {
            d1.close();
            d2.close();
            d3.close();
        }
    }
}
