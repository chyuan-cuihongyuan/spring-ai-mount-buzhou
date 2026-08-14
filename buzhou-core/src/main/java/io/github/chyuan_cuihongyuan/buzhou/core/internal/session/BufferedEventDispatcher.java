package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBusStats;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * impl-34 / spec 13 §core-4：有界异步事件分发器（opt-in {@code buffered} 模式的执行体）。
 *
 * <p>事件入 {@link ArrayBlockingQueue}，专属虚拟线程（{@code buzhou-event-dispatch-<sessionId>}）
 * 顺序排空并经 {@code deliver} 回调交付（回调内已含逐监听器异常隔离）。容量打满按
 * {@link io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.OverflowPolicy}
 * 处理；<b>丢弃必须计数可见</b>——累计计数 + 每 {@link EventDispatchConfig#DROP_SUMMARY_EVERY}
 * 次丢弃输出一条 WARN 汇总（Akka 死信语义）。
 *
 * <p>关闭语义（{@link #close()}）：投递毒丸 → 宽限预算内等排空（尽力而为，不无限阻塞）→
 * 到点中断分发线程、滞留事件计数为丢弃。close 后的 {@link #enqueue} 拒绝并计丢弃。
 */
final class BufferedEventDispatcher implements AutoCloseable {

    private static final SessionEvent POISON = new SessionEvent("poison", java.util.Map.of(), null);
    /** close 排空宽限上界：与 pushTimeout 解耦的固定预算（事件交付通常毫秒级）。 */
    private static final long DRAIN_BUDGET_MILLIS = 5_000L;

    private static final System.Logger LOGGER =
            System.getLogger(BufferedEventDispatcher.class.getName());

    private final String sessionId;
    private final EventDispatchConfig config;
    private final Consumer<SessionEvent> deliver;
    private final ArrayBlockingQueue<SessionEvent> queue;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong dispatched = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final Thread drainer;

    BufferedEventDispatcher(String sessionId, EventDispatchConfig config,
                            Consumer<SessionEvent> deliver) {
        this.sessionId = sessionId;
        this.config = config;
        this.deliver = deliver;
        this.queue = new ArrayBlockingQueue<>(config.capacity());
        this.drainer = BuzhouThreadFactory.virtual("event-dispatch").newThread(this::drainLoop);
        this.drainer.start();
    }

    /** 入队（溢出按策略处理；丢弃计数 + 低频汇总）。close 后拒绝入队并计丢弃。 */
    void enqueue(SessionEvent event) {
        if (closed.get()) {
            countDrop(event, "dispatcher-closed");
            return;
        }
        enqueued.incrementAndGet();
        if (queue.offer(event)) {
            return;
        }
        if (config.overflow() == EventDispatchConfig.OverflowPolicy.BLOCK) {
            try {
                if (queue.offer(event, config.pushTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                    return;
                }
                countDrop(event, "block-timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                countDrop(event, "interrupted");
            }
            return;
        }
        // DROP_OLDEST：挤掉队首最老事件再入队（竞态下二次失败仍丢弃——诚实计数）
        SessionEvent evicted = queue.poll();
        if (evicted != null && evicted != POISON) {
            countDrop(evicted, "drop-oldest");
        }
        if (!queue.offer(event)) {
            countDrop(event, "drop-oldest-race");
        }
    }

    private void countDrop(SessionEvent event, String reason) {
        long total = dropped.incrementAndGet();
        // impl-41 / spec 13 §T66：丢弃可见性指标（与累计计数同源）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.eventbus.dropped");
        if (event != null && event != POISON
                && (total == 1 || total % EventDispatchConfig.DROP_SUMMARY_EVERY == 0)) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "事件溢出丢弃（sessionId={0}, type={1}, reason={2}, droppedTotal={3}, queueCapacity={4}）",
                    sessionId, event.type(), reason, total, config.capacity());
        }
    }

    private void drainLoop() {
        try {
            while (true) {
                SessionEvent event = queue.take();
                if (event == POISON) {
                    return;
                }
                deliver.accept(event);
                dispatched.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // close 宽限超时的硬截断：滞留事件由 close 计丢弃
        }
    }

    EventBusStats stats() {
        return new EventBusStats(dispatched.get(), dropped.get(), enqueued.get(), queue.size());
    }

    /** 关闭：毒丸 → 宽限排空 → 硬截断。滞留事件计数为丢弃（可见）。 */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            queue.offer(POISON);
            drainer.join(DRAIN_BUDGET_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (drainer.isAlive()) {
            drainer.interrupt();
        }
        // 滞留队列的事件不会再被交付：诚实计数为丢弃
        queue.removeIf(e -> {
            if (e != POISON) {
                countDrop(e, "closed-undelivered");
                return true;
            }
            return false;
        });
    }
}
