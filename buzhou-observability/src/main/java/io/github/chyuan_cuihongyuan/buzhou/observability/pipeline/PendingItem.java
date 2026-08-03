package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

/**
 * 异步管线待落库项（密封接口，区分 span / event / snapshot / flush token）。
 *
 * <p>span 与 event 经 {@link io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore}
 * 的批量接口落库；snapshot 单条；flush token 触发批量 drain（无自身数据）。
 */
public sealed interface PendingItem permits PendingSpan, PendingEvent, PendingSnapshot, FlushToken {
}
