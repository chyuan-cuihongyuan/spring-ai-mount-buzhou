package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
/** spec 1529 / T2309：溢写存储 SPI——落盘/回读/清扫/校验的持久化契约。 */

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
