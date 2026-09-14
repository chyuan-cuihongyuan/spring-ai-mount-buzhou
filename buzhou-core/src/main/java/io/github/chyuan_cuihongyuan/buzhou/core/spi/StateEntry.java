package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;

/**
 * 状态条目——key / value / producer / 轮次号 / 过期时刻（SessionStateStore 载荷）。
 */
public record StateEntry(String key, String value, String producer,
                         int createdTurn, Integer ttlTurns, Instant updatedAt) {
}
