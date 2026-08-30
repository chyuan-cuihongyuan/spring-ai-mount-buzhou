package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 在飞工具调用合并器（spec 139 / T487，Hystrix request collapsing 借鉴）：
 * 同键<b>在飞</b>请求共享同一 Future——执行一次、全部等待者得同值；<b>完成即忘</b>
 * （终态即移除键——合并≠缓存，不持任何历史）；失败传播所有等待者。
 *
 * <p>key 由调用方构造（建议 工具名 + argsHash）。执行线程 = 调用方 executor
 * （本类不自持线程）。coalesced 计数 = 被折叠掉的重复请求数。
 */
public final class ToolCallCoalescer {

    private static final String COALESCE_COUNTER = "buzhou.tool-coalesce.coalesced";

    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlight =
            new ConcurrentHashMap<>();
    private final AtomicLong coalesced = new AtomicLong();

    /**
     * 提交（或加入）一个在飞调用：首达者执行，并发同键者共享其 Future。
     *
     * @return 共享 Future（成值或同败）
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submit(String key, Callable<T> task,
                                           ExecutorService executor) {
        if (key == null || key.isBlank() || task == null || executor == null) {
            throw new IllegalArgumentException("key/task/executor 必须非空");
        }
        CompletableFuture<Object> created = new CompletableFuture<>();
        CompletableFuture<Object> mine = created;
        CompletableFuture<Object> shared = inFlight.computeIfAbsent(key, k -> {
            executor.submit(() -> {
                try {
                    mine.complete(task.call());
                } catch (Throwable t) {
                    mine.completeExceptionally(t);
                }
            });
            return mine;
        });
        if (shared != mine) {
            coalesced.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter(COALESCE_COUNTER, 1);
        }
        // 完成即忘（CAS 同款 Future 才移除——防与后浪新建竞态误删）
        shared.whenComplete((r, e) -> inFlight.remove(key, shared));
        return (CompletableFuture<T>) shared;
    }

    /** 被折叠掉的重复请求总数（观测面）。 */
    public long coalescedCount() {
        return coalesced.get();
    }

    /** 当前在飞键数（观测面）。 */
    public int inFlightCount() {
        return inFlight.size();
    }
}