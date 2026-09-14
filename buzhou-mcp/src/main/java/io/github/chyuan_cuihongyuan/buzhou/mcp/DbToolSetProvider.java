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
    // 同 R46–R57 先例）。双入口（轮询 checkQuietly / 写后直调 listener）共同汇入
    // checkAndFire 三结局桶。守恒：polls + pushRefreshes =
    // changesDetected + unchangedRefreshes + refreshFailures（每入口恰落一桶）。
    private static final AtomicLong POLLS = new AtomicLong();
    private static final AtomicLong PUSH_REFRESHES = new AtomicLong();
    private static final AtomicLong CHANGES_DETECTED = new AtomicLong();
    private static final AtomicLong UNCHANGED_REFRESHES = new AtomicLong();
    private static final AtomicLong REFRESH_FAILURES = new AtomicLong();

    /** 轮询健康分布快照（spec 1058）。 */
    public record ToolSetPollStats(long polls, long pushRefreshes, long changesDetected,
                                   long unchangedRefreshes, long refreshFailures) {
    }

    /** 只读快照（守恒 polls + pushRefreshes = 三结局桶之和）。 */
    public static ToolSetPollStats stats() {
        return new ToolSetPollStats(POLLS.get(), PUSH_REFRESHES.get(), CHANGES_DETECTED.get(),
                UNCHANGED_REFRESHES.get(), REFRESH_FAILURES.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        POLLS.set(0);
        PUSH_REFRESHES.set(0);
        CHANGES_DETECTED.set(0);
        UNCHANGED_REFRESHES.set(0);
        REFRESH_FAILURES.set(0);
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
            mem.addWriteListener(() -> {
                PUSH_REFRESHES.incrementAndGet();
                checkAndFire();
            });
        }
        poller.scheduleWithFixedDelay(this::checkQuietly,
                pollInterval.toMillis(), pollInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void checkQuietly() {
        POLLS.incrementAndGet();
        try {
            checkAndFire();
        } catch (RuntimeException ignored) {
            // 存储抖动不炸轮询线程，下轮重试（失败已入 refreshFailures 桶）
        }
    }

    private void checkAndFire() {
        List<ToolSetSpec> current;
        try {
            current = List.copyOf(store.loadAll());
        } catch (RuntimeException e) {
            // 失败入桶后按原语义抛出：轮询路径由 checkQuietly 吞掉，直调路径照旧外溢
            REFRESH_FAILURES.incrementAndGet();
            throw e;
        }
        if (!current.equals(snapshot)) {
            snapshot = current;
            CHANGES_DETECTED.incrementAndGet();
            listeners.forEach(Runnable::run);
        } else {
            UNCHANGED_REFRESHES.incrementAndGet();
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
