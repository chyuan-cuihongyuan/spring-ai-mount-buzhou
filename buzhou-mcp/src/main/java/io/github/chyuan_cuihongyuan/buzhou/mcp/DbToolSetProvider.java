package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DB 清单源（spec 04）：读持久层，后台改配即推送。
 *
 * <p>推送 = 轮询 {@link ToolSetSpecStore} 比对快照，差异时回调监听器（{@link McpClientRegistry}
 * 差量刷新只动变化项，故粗粒度「有变化」通知即可）；{@link InMemoryToolSetSpecStore} 的写后
 * 即时通知经可选构造参数接线，免等轮询。
 *
 * <p>多实例一致性等级未定（spec 04 开放问题）：各实例各自轮询，短期清单不一致可接受。
 */
public class DbToolSetProvider implements ToolSetProvider, AutoCloseable {

    private final ToolSetSpecStore store;
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService poller;
    private volatile List<ToolSetSpec> snapshot;

    // —— spec 1058 / impl 810：轮询健康读面（etcd watch statistics 思想；静态面理由
    // 同 R46–R57 先例）。守恒：polls = changesDetected + unchangedPolls + pollFailures。
    private static final AtomicLong POLLS = new AtomicLong();
    private static final AtomicLong CHANGES_DETECTED = new AtomicLong();
    private static final AtomicLong UNCHANGED_POLLS = new AtomicLong();
    private static final AtomicLong POLL_FAILURES = new AtomicLong();

    /** 轮询健康分布快照（spec 1058）。 */
    public record ToolSetPollStats(long polls, long changesDetected,
                                   long unchangedPolls, long pollFailures) {
    }

    /** 只读快照（守恒 polls = 三桶之和）。 */
    public static ToolSetPollStats stats() {
        return new ToolSetPollStats(POLLS.get(), CHANGES_DETECTED.get(),
                UNCHANGED_POLLS.get(), POLL_FAILURES.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        POLLS.set(0);
        CHANGES_DETECTED.set(0);
        UNCHANGED_POLLS.set(0);
        POLL_FAILURES.set(0);
    }

    /** @param pollInterval 轮询间隔（默认 5s；测试可调小） */
    public DbToolSetProvider(ToolSetSpecStore store, Duration pollInterval) {
        this.store = store;
        this.snapshot = List.copyOf(store.loadAll());
        this.poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "buzhou-mcp-toolset-poller");
            t.setDaemon(true);
            return t;
        });
        if (store instanceof InMemoryToolSetSpecStore mem) {
            mem.addWriteListener(this::checkAndFire);
        }
        poller.scheduleWithFixedDelay(this::checkQuietly,
                pollInterval.toMillis(), pollInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void checkQuietly() {
        POLLS.incrementAndGet();
        try {
            checkAndFire();
        } catch (RuntimeException ignored) {
            // 存储抖动不炸轮询线程，下轮重试
            POLL_FAILURES.incrementAndGet();
        }
    }

    private void checkAndFire() {
        List<ToolSetSpec> current = List.copyOf(store.loadAll());
        if (!current.equals(snapshot)) {
            snapshot = current;
            CHANGES_DETECTED.incrementAndGet();
            listeners.forEach(Runnable::run);
        } else {
            UNCHANGED_POLLS.incrementAndGet();
        }
    }

    @Override
    public List<ToolSetSpec> currentToolSets() {
        return snapshot;
    }

    @Override
    public void addChangeListener(Runnable onChange) {
        listeners.add(onChange);
    }

    @Override
    public void close() {
        poller.shutdownNow();
    }
}
