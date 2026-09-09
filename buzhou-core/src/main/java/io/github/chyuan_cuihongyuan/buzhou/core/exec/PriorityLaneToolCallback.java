package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * 优先级泳道工具装饰器（spec 422 / T735，Envoy priority levels 借鉴——
 * 411 原语接线）：执行前按工具优先级取共享 {@link PriorityLane} 许可
 * （priority 数小者优先、同级 FIFO）、finally 归还（异常也归还）；超时
 * 抛 IllegalStateException（与 {@link LaneLimitingToolCallback} 同词汇——
 * 「工具泳道许可等待超时」）。定义透传，与 retry/memo/transform 可叠加。
 */
public final class PriorityLaneToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final PriorityLane lane;
    private final int priority;
    private final Duration acquireTimeout;

    /**
     * @param delegate       被装饰工具回调
     * @param lane           共享优先级泳道（{@code ToolLaneRegistry.priorityLane}
     *                       命名单例——跨会话共享容量）
     * @param priority       工具优先级（0-9 有界，数小者优先——越界
     *                       IllegalArgumentException 装配即红）
     * @param acquireTimeout 许可等待超时（正数——排队优先于拒绝）
     */
    public PriorityLaneToolCallback(ToolCallback delegate, PriorityLane lane,
                                    int priority, Duration acquireTimeout) {
        if (delegate == null || lane == null) {
            throw new IllegalArgumentException("delegate/lane 必须非空");
        }
        if (acquireTimeout == null || acquireTimeout.isZero() || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("acquireTimeout 为正");
        }
        this.delegate = delegate;
        this.lane = lane;
        this.priority = priority;
        this.acquireTimeout = acquireTimeout;
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

    private String withLane(Callable<String> execution) {
        try {
            lane.acquire(priority, acquireTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("泳道等待被中断", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("工具泳道许可等待超时（泳道满——容量调参入口）", e);
        }
        try {
            return execution.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("工具执行失败", e);
        } finally {
            lane.release();
        }
    }
}
