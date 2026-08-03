package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

/**
 * 待落库 event（{@link PendingItem} 之一）。
 */
public record PendingEvent(EventRecord record) implements PendingItem {
}
