package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 会话作用域的显式 SpanContext 载体（不用 ThreadLocal/ScopedValue，虚拟线程抗串味）。
 *
 * <p><b>背景</b>：Spring AI advisor 的 {@code request.context()} 与 {@code ToolCallingChatOptions.toolContext}
 * 是两套独立 map，advisor 无法把 SpanContext 自动透传给 ToolCallback。本载体作为会话级显式参数
 * 传递的补充：advisor 开 Turn/ModelCall span 后把 SpanContext 写入载体；ToolCallback 包装层从载体读。
 * 同时 {@code HarnessToolCallingManager} 构造 toolContext 时把载体写进 {@link #KEY}，供 ToolCallback
 * 经 {@code ToolContext} 取（双重通道，防御）。
 *
 * <p><b>并发抗串味</b>：fan-out 任务提交前由采集方调 {@link #snapshotTurn()}
 * 捕获当前 Turn 的 SpanContext（record 不可变），任务内用快照开 TOOL_CALL span，不读可变状态——
 * 同轮并发工具各自的 parent 均指向正确的 Turn span（spec 03 时序图：TOOL_CALL parent = TURN）。
 */
public final class SpanContextCarrier {

    /** {@code ToolContext.getContext()} 中存放本载体的 key。 */
    public static final String KEY = "__buzhou.spanContextCarrier";

    private final AtomicReference<SpanContext> currentTurn = new AtomicReference<>();
    private final AtomicInteger parallelIndex = new AtomicInteger();
    private volatile SpanContext sessionSpan;

    public void bindSessionSpan(SpanContext sessionSpan) {
        this.sessionSpan = sessionSpan;
    }

    public SpanContext sessionSpan() {
        return sessionSpan;
    }

    public void bindTurn(SpanContext turn) {
        currentTurn.set(turn);
        parallelIndex.set(0);
    }

    /** 当前 Turn 的 SpanContext（ToolCallback 包装层用作 TOOL_CALL 的 parent）。 */
    public SpanContext currentTurn() {
        return currentTurn.get();
    }

    /** 当前 Turn 的 SpanContext 快照；并发任务捕获此值开 TOOL_CALL span。 */
    public SpanContext snapshotTurn() {
        return currentTurn.get();
    }

    /** 分配一个并发序号（辅助属性 {@code tool.parallel.index}；并发抗串味靠显式 SpanContext 而非 index）。 */
    public int nextParallelIndex() {
        return parallelIndex.getAndIncrement();
    }

    public void clear() {
        currentTurn.set(null);
        parallelIndex.set(0);
        sessionSpan = null;
    }
}
