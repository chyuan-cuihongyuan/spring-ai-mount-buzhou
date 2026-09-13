package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.Map;

/**
 * impl-653 / spec 900：事件丢弃按原因分类快照（Sentry discarded events 借鉴——
 * 「为什么丢」一读即知，不只有总量）。
 *
 * <p>{@code buffered} 分发模式的丢弃现场在 {@code BufferedEventDispatcher.countDrop}
 * 单点累计：{@code drop-oldest}（容量不足挤掉队首）、{@code drop-oldest-race}（挤掉后
 * 二次入队仍失败的竞态）、{@code block-timeout}（BLOCK 策略入队超时）、
 * {@code interrupted}（入队等待被中断）、{@code dispatcher-closed}（关闭后仍入队）、
 * {@code closed-undelivered}（close 硬截断时的队列滞留）。守恒不变量：
 * {@link #total()} 恒等于 {@link EventBusStats#dropped()}（同一计数单点）。
 *
 * <p>仅 {@code buffered} 模式适用；SYNC 内联分发（默认）经
 * {@link AgentSession#eventDropBreakdown()} 返回空。
 *
 * @param byReason 丢弃原因 → 累计条数（不可变快照，只含已发生的原因）
 */
public record EventDropBreakdown(Map<String, Long> byReason) {

    /** 空快照（SYNC 模式 / 无丢弃语义占位）。 */
    public static final EventDropBreakdown EMPTY =
            new EventDropBreakdown(Map.of());

    public EventDropBreakdown {
        byReason = Map.copyOf(byReason);
    }

    /** 指定原因的累计条数（未发生的原因返回 0）。 */
    public long forReason(String reason) {
        return byReason.getOrDefault(reason, 0L);
    }

    /** 全原因合计（守恒不变量：恒等于 {@link EventBusStats#dropped()}）。 */
    public long total() {
        return byReason.values().stream().mapToLong(Long::longValue).sum();
    }
}
