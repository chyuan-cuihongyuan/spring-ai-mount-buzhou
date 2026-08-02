package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;

public record LeaseInfo(String ownerId, long fencingToken, Instant acquiredAt, Instant expiresAt) {
}
