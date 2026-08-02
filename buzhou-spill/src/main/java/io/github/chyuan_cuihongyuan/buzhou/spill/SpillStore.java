package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface SpillStore {

    SpillHandle store(SpillEntry entry, int previewChars);

    Optional<String> load(SpillUri uri);

    RangeReadResult readRange(SpillUri uri, RangeReadRequest request);

    void markLinked(SpillUri uri);

    void delete(SpillUri uri);

    int deleteBySession(String agentName, String sessionId);

    int deleteExpired(Instant now, Duration ttl);

    boolean exists(SpillUri uri);
}
