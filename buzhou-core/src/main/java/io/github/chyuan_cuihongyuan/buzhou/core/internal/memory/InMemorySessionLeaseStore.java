package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话租约内存实现。
 *
 * <p>impl-36 / spec 13 §growth-8 边界说明：租约随 release / steal 覆盖 / 过期惰性判定
 * <b>物理移除</b>（impl-33），活跃租约数天然受会话生命周期约束——不另设容量上限；
 * {@link #deleteSession} 移除终结后残条。
 */
public class InMemorySessionLeaseStore implements SessionLeaseStore {

    private final ConcurrentHashMap<String, LeaseInfo> leases = new ConcurrentHashMap<>();
    private final AtomicLong fencing = new AtomicLong();

    /**
     * impl-33 / spec 13 §core-3：在册租约条数（测试可观测性——验证过期租约被<b>物理移除</b>
     * 而非仅惰性判定失效）。internal 包，不属公开 API。
     */
    public int leaseCount() {
        return leases.size();
    }

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

    /**
     * impl-33 / spec 13 §core-3：renew 以 {@code compute} 原子判定——<b>过期租约视为不可再取</b>
     * （过期即物理移除并返回 false，而非复活续期），owner/fencingToken 不一致照旧 false。
     * 修复既有缺陷：原「get + replace」两步式在过期分支会复活已过期租约（语义违约）。
     */
    @Override
    public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
        boolean[] renewed = {false};
        leases.compute(sessionId, (k, current) -> {
            if (current == null) {
                return null;
            }
            if (current.expiresAt().isBefore(Instant.now())) {
                return null; // 过期 → 物理移除（compute 返回 null 即原子删除）
            }
            if (!current.ownerId().equals(ownerId) || current.fencingToken() != fencingToken) {
                return current;
            }
            renewed[0] = true;
            return new LeaseInfo(ownerId, fencingToken, current.acquiredAt(), Instant.now().plus(ttl));
        });
        return renewed[0];
    }

    @Override
    public void release(String sessionId, String ownerId, long fencingToken) {
        leases.computeIfPresent(sessionId, (k, existing) -> {
            if (existing.expiresAt().isBefore(Instant.now())) {
                return null; // 过期租约顺带物理移除（死条目不残留）
            }
            return existing.ownerId().equals(ownerId) && existing.fencingToken() == fencingToken
                    ? null : existing;
        });
    }

    @Override
    public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
        long token = fencing.incrementAndGet();
        leases.put(sessionId,
                new LeaseInfo(newOwnerId, token, Instant.now(), Instant.now().plus(ttl)));
        return new LeaseAcquireResult(true, token);
    }

    /**
     * impl-33 / spec 13 §core-3：inspect 惰性判定时对过期租约<b>物理移除</b>
     * （compute 原子判定——到期条目不再常驻内存，防长生命周期进程泄漏）。
     */
    @Override
    public Optional<LeaseInfo> inspect(String sessionId) {
        LeaseInfo info = leases.compute(sessionId, (k, current) ->
                current != null && current.expiresAt().isBefore(Instant.now()) ? null : current);
        return Optional.ofNullable(info);
    }

    /** impl-35 / spec 13 §stores-6：移除该会话租约（幂等；全局 fencing 计数器不复位——token 不复用语义）。 */
    @Override
    public void deleteSession(String sessionId) {
        leases.remove(sessionId);
    }
}
