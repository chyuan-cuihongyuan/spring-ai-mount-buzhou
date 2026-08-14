package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * impl-34 / spec 13 §core-4：事件总线运行时统计快照（丢弃必须计数可见）。
 *
 * <p>对应 {@link EventDispatchConfig#mode()}{@code = BUFFERED} 的会话；SYNC 会话不适用
 * （内联分发无队列、无丢弃）。数值为构造快照时刻的近似值（非强一致）。
 *
 * @param dispatched 已交付（hook 链 + listener 分发完成）的事件数
 * @param dropped    溢出丢弃的事件数（DROP_OLDEST 被挤掉 + BLOCK 入队超时）
 * @param enqueued   已入队总数（含已交付与滞留队列）
 * @param queueDepth 快照时刻的队列滞留深度
 */
public record EventBusStats(long dispatched, long dropped, long enqueued, int queueDepth) {

    static final EventBusStats EMPTY = new EventBusStats(0, 0, 0, 0);
}
