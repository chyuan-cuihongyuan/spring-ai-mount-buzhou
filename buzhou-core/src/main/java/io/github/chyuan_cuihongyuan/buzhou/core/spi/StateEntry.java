package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;

public record StateEntry(String key, String value, String producer,
                         int createdTurn, Integer ttlTurns, Instant updatedAt) {
}
