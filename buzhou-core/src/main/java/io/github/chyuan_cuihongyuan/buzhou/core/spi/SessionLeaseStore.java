package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Duration;
import java.util.Optional;

public interface SessionLeaseStore {

    LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl);

    boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl);

    void release(String sessionId, String ownerId, long fencingToken);

    LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl);

    Optional<LeaseInfo> inspect(String sessionId);

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的租约与 fencing 计数器。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }
}
