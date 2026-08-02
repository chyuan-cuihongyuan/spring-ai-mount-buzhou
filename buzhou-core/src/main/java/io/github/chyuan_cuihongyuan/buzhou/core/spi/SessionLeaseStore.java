package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Duration;
import java.util.Optional;

public interface SessionLeaseStore {

    LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl);

    boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl);

    void release(String sessionId, String ownerId, long fencingToken);

    LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl);

    Optional<LeaseInfo> inspect(String sessionId);
}
