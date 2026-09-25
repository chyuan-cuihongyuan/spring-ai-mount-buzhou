package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1406 / T2114：租约续期健康读面（Redisson watchdog 审计思想）——
 * 成功/失败双计数、续期时剩余租期最小水位（调度饿死收窄）、末次续期时刻、
 * 失败终态 lost、从未续期哨兵。
 */
class LeaseRenewalReadoutTest {

    private final InMemorySessionLeaseStore store = new InMemorySessionLeaseStore();

    private SessionLeaseGuard guardOf(String sessionId, Duration ttl) {
        LeaseAcquireResult acquire = store.tryAcquire(sessionId, "owner-a", ttl);
        assertThat(acquire.acquired()).isTrue();
        return new SessionLeaseGuard(store, sessionId, "owner-a",
                acquire.fencingToken(), ttl, ttl.dividedBy(3));
    }

    @Test
    void freshGuardReportsNeverRenewedSentinels() {
        SessionLeaseGuard guard = guardOf("s-lr-1", Duration.ofMillis(600));
        var stats = guard.renewalStats();
        assertThat(stats.renewals()).isZero();
        assertThat(stats.failures()).isZero();
        assertThat(stats.minRemainingAtRenewalMillis()).isEqualTo(-1);
        assertThat(stats.lastRenewalAtEpochMillis()).isZero();
        assertThat(stats.lost()).isFalse();
        assertThat(stats.ttlMillis()).isEqualTo(600);
        guard.close();
    }

    @Test
    void successfulRenewalRecordsWatermarkAndTimestamp() throws Exception {
        SessionLeaseGuard guard = guardOf("s-lr-2", Duration.ofMillis(600));
        Thread.sleep(250); // 剩余约 350ms 时续期——水位应显著小于 TTL
        assertThat(guard.renewQuietly()).isTrue();
        var stats = guard.renewalStats();
        assertThat(stats.renewals()).isEqualTo(1);
        assertThat(stats.failures()).isZero();
        assertThat(stats.minRemainingAtRenewalMillis()).isPositive().isLessThan(400);
        assertThat(stats.lastRenewalAtEpochMillis()).isPositive();
        assertThat(stats.lost()).isFalse();
        guard.close();
    }

    @Test
    void renewalFailureCountsAndMarksLost() throws Exception {
        SessionLeaseGuard guard = guardOf("s-lr-3", Duration.ofMillis(50));
        Thread.sleep(150); // store 侧过期物理移除 → renew false
        assertThat(guard.renewQuietly()).isFalse();
        var stats = guard.renewalStats();
        assertThat(stats.renewals()).isZero();
        assertThat(stats.failures()).isEqualTo(1);
        assertThat(stats.lost()).isTrue();
        // lost 后再续：快速 false，不重复计数（终态前恰一次）
        assertThat(guard.renewQuietly()).isFalse();
        assertThat(guard.renewalStats().failures()).isEqualTo(1);
    }

    @Test
    void consecutiveRenewalsKeepWatermarkMonotonic() {
        SessionLeaseGuard guard = guardOf("s-lr-4", Duration.ofMillis(600));
        assertThat(guard.renewQuietly()).isTrue();
        long first = guard.renewalStats().minRemainingAtRenewalMillis();
        assertThat(guard.renewQuietly()).isTrue();
        var stats = guard.renewalStats();
        assertThat(stats.renewals()).isEqualTo(2);
        // 立即续期时剩余≈TTL——水位不会被第二次更大的剩余值抬高。
        // spec 6023 环境确定性清零：跨毫秒时钟粒度下第二次续约
        // 剩余可比 first 小 1ms（min 取下确界）——合同本意是
        // 「min 不被抬高」，改 ≤ 断言。
        assertThat(stats.minRemainingAtRenewalMillis()).isLessThanOrEqualTo(first);
        assertThat(first).isLessThanOrEqualTo(600).isGreaterThan(0);
        guard.close();
    }
}
