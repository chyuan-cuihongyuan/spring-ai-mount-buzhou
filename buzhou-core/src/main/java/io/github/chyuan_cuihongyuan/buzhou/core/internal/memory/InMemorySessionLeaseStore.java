package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemorySessionLeaseStore implements SessionLeaseStore {

    private final ConcurrentHashMap<String, LeaseInfo> leases = new ConcurrentHashMap<>();
    private final AtomicLong fencing = new AtomicLong();

    @Override
    public LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl) {
        LeaseInfo[] holder = new LeaseInfo[1];
        leases.compute(sessionId, (k, existing) -> {
            if (existing == null || existing.expiresAt().isBefore(Instant.now())) {
                holder[0] = new LeaseInfo(ownerId, fencing.incrementAndGet(),
                        Instant.now(), Instant.now().plus(ttl));
                return holder[0];
            }
            return existing;
        });
        return holder[0] != null
                ? new LeaseAcquireResult(true, holder[0].fencingToken())
                : new LeaseAcquireResult(false, leases.get(sessionId).fencingToken());
    }

    @Override
    public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
        LeaseInfo current = leases.get(sessionId);
        if (current == null || !current.ownerId().equals(ownerId)
                || current.fencingToken() != fencingToken) {
            return false;
        }
        return leases.replace(sessionId, current,
                new LeaseInfo(ownerId, fencingToken, current.acquiredAt(), Instant.now().plus(ttl)));
    }

    @Override
    public void release(String sessionId, String ownerId, long fencingToken) {
        leases.computeIfPresent(sessionId, (k, existing) ->
                existing.ownerId().equals(ownerId) && existing.fencingToken() == fencingToken
                        ? null : existing);
    }

    @Override
    public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
        long token = fencing.incrementAndGet();
        leases.put(sessionId,
                new LeaseInfo(newOwnerId, token, Instant.now(), Instant.now().plus(ttl)));
        return new LeaseAcquireResult(true, token);
    }

    @Override
    public Optional<LeaseInfo> inspect(String sessionId) {
        LeaseInfo info = leases.get(sessionId);
        if (info == null || info.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(info);
    }
}
