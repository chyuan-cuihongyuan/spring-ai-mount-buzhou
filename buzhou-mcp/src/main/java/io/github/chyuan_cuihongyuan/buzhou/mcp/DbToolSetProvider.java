package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        try {
            checkAndFire();
        } catch (RuntimeException ignored) {
            // 存储抖动不炸轮询线程，下轮重试
        }
    }

    private void checkAndFire() {
        List<ToolSetSpec> current = List.copyOf(store.loadAll());
        if (!current.equals(snapshot)) {
            snapshot = current;
            listeners.forEach(Runnable::run);
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
