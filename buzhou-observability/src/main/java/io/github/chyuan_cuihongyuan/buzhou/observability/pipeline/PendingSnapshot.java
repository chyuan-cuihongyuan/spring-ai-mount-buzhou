package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;

/**
 * 待落库注入快照（{@link PendingItem} 之一）。
 */
public record PendingSnapshot(InjectionSnapshot record) implements PendingItem {
}
