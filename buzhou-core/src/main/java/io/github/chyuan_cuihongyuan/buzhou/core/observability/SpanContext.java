package io.github.chyuan_cuihongyuan.buzhou.core.observability;

/**
 * 显式传递的 Span 上下文（不用 ThreadLocal/ScopedValue，虚拟线程抗串味）。
 *
 * <p>采集挂接点（advisor、ToolCallback 包装）经会话级 {@link SpanContextCarrier} 读取当前
 * Turn / ModelCall 的 SpanContext，作为子 span 的 parent；并发 fan-out 任务捕获的是不可变快照，
 * 任务内不再读可变状态，故同轮并发工具各开 ToolCall span 且 parent 均正确指向所属 Turn。
 *
 * @param spanId    span 标识（UUID）
 * @param sessionId 会话标识
 * @param turnSeq   所属轮次序号；Session span 为 0
 */
public record SpanContext(String spanId, String sessionId, int turnSeq) {
}
