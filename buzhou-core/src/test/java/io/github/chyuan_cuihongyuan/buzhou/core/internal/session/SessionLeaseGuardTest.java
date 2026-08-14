package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-33 / spec 13 §core-3：单会话租约哨兵单元测试——续租节奏（剩余 &lt; 阈值才续）、
 * renew 失败 / fencingToken 不匹配 → {@link LeaseLostException} 并标记丢失、
 * 后台续租静默标记、fence 通过路径。计时语义用「阈值 &gt; TTL 的必续配置」避免真实时序 flake。
 */
class SessionLeaseGuardTest {

    /** 记录型 store：委托内存实现，计数 renew/inspect 供节奏断言。 */
    static final class RecordingLeaseStore implements SessionLeaseStore {
        final SessionLeaseStore delegate = new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore();
        final AtomicInteger renewCalls = new AtomicInteger();
        final AtomicInteger inspectCalls = new AtomicInteger();

        @Override
        public LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl) {
            return delegate.tryAcquire(sessionId, ownerId, ttl);
        }

        @Override
        public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
            renewCalls.incrementAndGet();
            return delegate.renew(sessionId, ownerId, fencingToken, ttl);
        }

        @Override
        public void release(String sessionId, String ownerId, long fencingToken) {
            delegate.release(sessionId, ownerId, fencingToken);
        }

        @Override
        public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
            return delegate.steal(sessionId, newOwnerId, ttl);
        }

        @Override
        public Optional<LeaseInfo> inspect(String sessionId) {
            inspectCalls.incrementAndGet();
            return delegate.inspect(sessionId);
        }
    }

    /** 阈值 &gt; TTL：本地剩余恒小于阈值 → renewIfDue 必然尝试续租（免真实时序）。 */
    private static SessionLeaseGuard alwaysDueGuard(RecordingLeaseStore store, String sessionId) {
        LeaseAcquireResult lease = store.tryAcquire(sessionId, "owner-A", Duration.ofSeconds(10));
        return new SessionLeaseGuard(store, sessionId, "owner-A", lease.fencingToken(),
                Duration.ofSeconds(10), Duration.ofSeconds(60));
    }

    @Test
    void shouldRenewAndExtendLease_whenRemainingBelowThreshold() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-renew");

        guard.renewIfDue();

        assertThat(store.renewCalls.get()).isEqualTo(1);
        assertThat(guard.renewalCount()).isEqualTo(1);
        assertThat(guard.isLost()).isFalse();
        // fencingToken 不变（续租非重取）
        assertThat(store.inspect("sess-renew")).isPresent()
                .get().extracting(LeaseInfo::fencingToken).isEqualTo(guard.fencingToken());
    }

    @Test
    void shouldSkipStoreRoundTrip_whenRemainingAboveThreshold() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        // 阈值远小于 TTL：本地剩余充足 → 不触碰 store
        LeaseAcquireResult lease = store.tryAcquire("sess-idle", "owner-A", Duration.ofSeconds(60));
        SessionLeaseGuard guard = new SessionLeaseGuard(store, "sess-idle", "owner-A",
                lease.fencingToken(), Duration.ofSeconds(60), Duration.ofMillis(100));

        guard.renewIfDue();

        assertThat(store.renewCalls.get()).isZero();
        assertThat(guard.renewalCount()).isZero();
    }

    @Test
    void shouldThrowLeaseLostAndMarkGuard_whenRenewFailsBecauseLeaseStolen() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-stolen");
        store.steal("sess-stolen", "owner-B", Duration.ofSeconds(90));

        assertThatThrownBy(guard::renewIfDue)
                .isInstanceOf(LeaseLostException.class)
                .extracting(e -> ((LeaseLostException) e).errorCode())
                .isEqualTo(ErrorCode.LEASE_LOST);
        assertThat(guard.isLost()).isTrue();
        // 丢失后再次轮间裁决即刻拒绝（不复活）
        assertThatThrownBy(guard::renewIfDue).isInstanceOf(LeaseLostException.class);
    }

    @Test
    void shouldThrowLeaseLost_whenFencingTokenMismatchAtFence() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-fence");
        store.steal("sess-fence", "owner-B", Duration.ofSeconds(90));

        assertThatThrownBy(guard::checkFence).isInstanceOf(LeaseLostException.class);
        assertThat(guard.isLost()).isTrue();
    }

    @Test
    void shouldPassFence_whenFencingTokenStillHeld() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-held");

        assertThatCode(guard::checkFence).doesNotThrowAnyException();
        assertThat(guard.isLost()).isFalse();
    }

    @Test
    void shouldMarkLostQuietlyWithoutThrowing_whenBackgroundRenewFails() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-bg");
        store.steal("sess-bg", "owner-B", Duration.ofSeconds(90));

        assertThat(guard.renewQuietly()).isFalse();
        assertThat(guard.isLost()).isTrue();
        // 已丢失：后台续租短路，不再打 store
        assertThat(guard.renewQuietly()).isFalse();
        assertThat(store.renewCalls.get()).isEqualTo(1);
    }

    @Test
    void shouldKeepLeaseAlive_whenBackgroundRenewSucceeds() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-alive");

        assertThat(guard.renewQuietly()).isTrue();
        assertThat(guard.renewalCount()).isEqualTo(1);
        assertThat(guard.isLost()).isFalse();
    }

    @Test
    void shouldFenceAndRenewInOneRound_whenBeforeRoundInvoked() {
        RecordingLeaseStore store = new RecordingLeaseStore();
        SessionLeaseGuard guard = alwaysDueGuard(store, "sess-round");

        guard.beforeRound();

        assertThat(store.inspectCalls.get()).isEqualTo(1); // fence 先行
        assertThat(store.renewCalls.get()).isEqualTo(1);   // 剩余不足随后续租
    }
}
