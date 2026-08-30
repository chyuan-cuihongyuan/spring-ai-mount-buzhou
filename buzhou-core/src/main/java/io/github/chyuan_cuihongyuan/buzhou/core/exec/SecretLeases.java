package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凭证租约仓（spec 153 / T511，Vault dynamic secrets 借鉴）：per-name 短命凭证——
 * issue 签发（同名重签覆盖，旧值立即失效）/ resolve 惰性过期 / renew 续租
 * （<b>过期即拒</b>——需重新签发，防旧凭证无限复活）/ revoke 即吊销。
 *
 * <p>明文 value 仅内存（外部 KMS/Vault 对接留档）。Clock 注入；per-name 单锁。
 */
public final class SecretLeases {

    private record Lease(String leaseId, String value, long expireAtMillis) {
    }

    private final Clock clock;
    private final Map<String, Lease> leases = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong issued =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong expired =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong revoked =
            new java.util.concurrent.atomic.AtomicLong();

    public SecretLeases() {
        this(Clock.systemUTC());
    }

    public SecretLeases(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /** 签发（返回 leaseId；同名重签覆盖旧租约——旧值立即失效）。 */
    public String issue(String name, String value, Duration ttl) {
        if (name == null || name.isBlank() || value == null
                || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("name/value 非空、ttl 为正");
        }
        String leaseId = UUID.randomUUID().toString();
        leases.put(name, new Lease(leaseId, value, clock.millis() + ttl.toMillis()));
        issued.incrementAndGet();
        return leaseId;
    }

    /** 解析：租期内得值；过期惰性剔除（expired 计数）→ empty。 */
    public Optional<String> resolve(String name) {
        Lease lease = leases.get(name);
        if (lease == null) {
            return Optional.empty();
        }
        synchronized (lease) {
            if (clock.millis() >= lease.expireAtMillis()) {
                leases.remove(name, lease);
                expired.incrementAndGet();
                return Optional.empty();
            }
            return Optional.of(lease.value());
        }
    }

    /** 续租（仅租期内可续；已过期抛 IllegalStateException——需重新签发）。 */
    public void renew(String name, Duration ttl) {
        Lease lease = leases.get(name);
        if (lease == null) {
            throw new IllegalStateException("租约不存在（需重新签发）：" + name);
        }
        synchronized (lease) {
            if (clock.millis() >= lease.expireAtMillis()) {
                leases.remove(name, lease);
                expired.incrementAndGet();
                throw new IllegalStateException("租约已过期（需重新签发）：" + name);
            }
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                throw new IllegalArgumentException("ttl 为正");
            }
            Lease extended = new Lease(lease.leaseId(), lease.value(),
                    clock.millis() + ttl.toMillis());
            leases.put(name, extended);
        }
    }

    /** 吊销（泄漏应急面——即刻失效，幂等）。 */
    public void revoke(String name) {
        if (leases.remove(name) != null) {
            revoked.incrementAndGet();
        }
    }

    /** 活跃租约名单（不含值——观测面安全）。 */
    public Set<String> activeLeases() {
        Set<String> out = new TreeSet<>();
        leases.forEach((name, lease) -> {
            if (clock.millis() < lease.expireAtMillis()) {
                out.add(name);
            } else {
                leases.remove(name, lease);
                expired.incrementAndGet();
            }
        });
        return out;
    }

    public long issuedCount() {
        return issued.get();
    }

    public long expiredCount() {
        return expired.get();
    }

    public long revokedCount() {
        return revoked.get();
    }
}
