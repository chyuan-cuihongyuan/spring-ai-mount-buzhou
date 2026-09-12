package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 泳道限流工具装饰器（spec 173 / T541，Hystrix 线程池隔离舱借鉴）：执行前取
 * 泳道许可（阻塞等待带超时——排队优先于拒绝；超时抛 IllegalStateException →
 * harness 既有错误反馈词汇），finally 归还（异常也归还）。定义透传，
 * 与 retry/memo/transform 可叠加；不包零变化。
 *
 * <p>spec 722 / T995：排队时延观测——acquire 段与执行段分开计量
 * （{@link #waitStats()} + {@code buzhou.lane.wait}/{@code buzhou.lane.timeout}），
 * 「Turn 慢是否泳道容量不足」可归因。
 */
public final class LaneLimitingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final Semaphore lane;
    private final String laneName;
    private final Duration acquireTimeout;
    /** spec 316 / T623：后端泳道许可（非 null 时走共享路径——Semaphore 路径零变化）。 */
    private final BackendLanePermit backendPermit;

    private LaneLimitingToolCallback(ToolCallback delegate, Semaphore lane,
                                     Duration acquireTimeout) {
        this(delegate, lane, acquireTimeout, null);
    }

    private LaneLimitingToolCallback(ToolCallback delegate, Semaphore lane,
                                     Duration acquireTimeout, BackendLanePermit backendPermit) {
        this(delegate, lane, null, acquireTimeout, backendPermit);
    }

    private LaneLimitingToolCallback(ToolCallback delegate, Semaphore lane, String laneName,
                                     Duration acquireTimeout, BackendLanePermit backendPermit) {
        this.delegate = delegate;
        this.lane = lane;
        this.laneName = laneName;
        this.acquireTimeout = acquireTimeout;
        this.backendPermit = backendPermit;
    }

    public static LaneLimitingToolCallback wrap(ToolCallback delegate, String laneName,
                                                int permits, ToolLaneRegistry registry,
                                                Duration acquireTimeout) {
        if (delegate == null || registry == null) {
            throw new IllegalArgumentException("delegate/registry 必须非空");
        }
        if (acquireTimeout == null || acquireTimeout.isZero() || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout 为正");
        }
        return new LaneLimitingToolCallback(delegate,
                registry.lane(laneName, permits), laneName, acquireTimeout, null);
    }

    /**
     * 共享泳道包装（spec 316 / T623）：许可面走 {@link BackendLanePermit}（跨实例
     * 共享——多实例一套泳道容量）；超时/异常词汇与 Semaphore 路径一致。
     */
    public static LaneLimitingToolCallback wrap(ToolCallback delegate,
                                                BackendLanePermit backendPermit,
                                                Duration acquireTimeout) {
        if (delegate == null || backendPermit == null) {
            throw new IllegalArgumentException("delegate/backendPermit 必须非空");
        }
        if (acquireTimeout == null || acquireTimeout.isZero() || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout 为正");
        }
        return new LaneLimitingToolCallback(delegate, null, acquireTimeout, backendPermit);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return withLane(() -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return withLane(() -> delegate.call(toolInput, toolContext));
    }

    private String withLane(java.util.concurrent.Callable<String> execution) {
        boolean acquired = false;
        // spec 722 / T995：排队时延观测——acquire 段与执行段分开计量（容量调参依据）
        long waitStart = System.nanoTime();
        try {
            if (backendPermit != null) {
                acquired = backendPermit.tryAcquire(acquireTimeout);
            } else {
                acquired = lane.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            recordWait(waitStart, acquired);
            if (!acquired) {
                throw new IllegalStateException("工具泳道许可等待超时（泳道满——容量调参入口）");
            }
            return execution.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("泳道等待被中断", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("工具执行失败", e);
        } finally {
            if (acquired) {
                if (backendPermit != null) {
                    backendPermit.release();
                } else {
                    lane.release();
                }
            }
        }
    }

    /** 泳道等待统计（观测 record——实例面）。 */
    public record WaitStats(long waited, long totalWaitNanos, long maxWaitNanos, long timeouts) {
    }

    private final AtomicLong waitedCount = new AtomicLong();
    private final AtomicLong totalWaitNanos = new AtomicLong();
    private final AtomicLong maxWaitNanos = new AtomicLong();
    private final AtomicLong timeoutCount = new AtomicLong();

    private void recordWait(long waitStart, boolean acquired) {
        long waited = System.nanoTime() - waitStart;
        waitedCount.incrementAndGet();
        totalWaitNanos.addAndGet(waited);
        maxWaitNanos.updateAndGet(prev -> Math.max(prev, waited));
        String laneTag = laneName;
        if (laneTag != null) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .timer("buzhou.lane.wait", java.time.Duration.ofNanos(waited),
                            "lane", laneTag);
        }
        if (!acquired) {
            timeoutCount.incrementAndGet();
            if (laneTag != null) {
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                        .counter("buzhou.lane.timeout", "lane", laneTag);
            }
        }
    }

    /** spec 722：排队等待统计（观测面——Turn 慢的泳道归因）。 */
    public WaitStats waitStats() {
        return new WaitStats(waitedCount.get(), totalWaitNanos.get(),
                maxWaitNanos.get(), timeoutCount.get());
    }
}
