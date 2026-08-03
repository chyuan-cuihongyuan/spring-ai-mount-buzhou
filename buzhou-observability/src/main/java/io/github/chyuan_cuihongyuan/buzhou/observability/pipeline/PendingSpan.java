package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

/**
 * 待落库 span（{@link PendingItem} 之一）。
 */
public record PendingSpan(SpanRecord record) implements PendingItem {
}
