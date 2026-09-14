package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBusStats;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDropBreakdown;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
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

    // impl-671 / spec 918：丢弃原因常量集（值域封闭——WARN 文本/分类键/指标 tag 三处同源）
    static final String DROP_REASON_OLDEST = "drop-oldest";
    static final String DROP_REASON_OLDEST_RACE = "drop-oldest-race";
    static final String DROP_REASON_BLOCK_TIMEOUT = "block-timeout";
    static final String DROP_REASON_INTERRUPTED = "interrupted";
    static final String DROP_REASON_DISPATCHER_CLOSED = "dispatcher-closed";
    static final String DROP_REASON_CLOSED_UNDELIVERED = "closed-undelivered";

    private final String sessionId;
    private final EventDispatchConfig config;
    private final Consumer<SessionEvent> deliver;
    private final ArrayBlockingQueue<SessionEvent> queue;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong dispatched = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    /** impl-653 / spec 900：按原因分类计数（与 dropped 同点累计——守恒不变量）。 */
    private final ConcurrentHashMap<String, LongAdder> dropsByReason = new ConcurrentHashMap<>();
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
            countDrop(event, DROP_REASON_DISPATCHER_CLOSED);
            return;
        }
        enqueued.incrementAndGet();
        if (queue.offer(event)) {
            noteDepth();
            return;
        }
        if (config.overflow() == EventDispatchConfig.OverflowPolicy.BLOCK) {
            // spec 1415 / T2131：首推失败进入限时等待 = 背压发生（容量打满信号）
            io.github.chyuan_cuihongyuan.buzhou.core.session.EventBackpressureStats
                    .recordBlockedPush();
            try {
                if (queue.offer(event, config.pushTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                    noteDepth();
                    return;
                }
                countDrop(event, DROP_REASON_BLOCK_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                countDrop(event, DROP_REASON_INTERRUPTED);
            }
            return;
        }
        // DROP_OLDEST：挤掉队首最老事件再入队（竞态下二次失败仍丢弃——诚实计数）
        SessionEvent evicted = queue.poll();
        if (evicted != null && evicted != POISON) {
            countDrop(evicted, DROP_REASON_OLDEST);
        }
        if (!queue.offer(event)) {
            countDrop(event, DROP_REASON_OLDEST_RACE);
        }
        noteDepth();
    }

    /** spec 1415 / T2131：队列深度采样进进程级水位（只增记账，行为零变化）。 */
    private void noteDepth() {
        io.github.chyuan_cuihongyuan.buzhou.core.session.EventBackpressureStats
                .recordDepth(queue.size());
    }

    private void countDrop(SessionEvent event, String reason) {
        long total = dropped.incrementAndGet();
        dropsByReason.computeIfAbsent(reason, k -> new LongAdder()).increment();
        // impl-41 / spec 13 §T66：丢弃可见性指标（与累计计数同源）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.eventbus.dropped");
        // impl-671 / spec 918：reason 维度序列（值域封闭 6 常量——基数天然有界；
        // 与总量 counter 双轨并存，既有面板序列不分裂）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.eventbus.dropped-reason", "reason", reason);
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

    /** impl-653 / spec 900：按原因分类的丢弃快照（守恒：total() == stats().dropped()）。 */
    EventDropBreakdown dropBreakdown() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        dropsByReason.forEach((reason, adder) -> snapshot.put(reason, adder.sum()));
        return new EventDropBreakdown(snapshot);
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
                countDrop(e, DROP_REASON_CLOSED_UNDELIVERED);
                return true;
            }
            return false;
        });
    }
}
