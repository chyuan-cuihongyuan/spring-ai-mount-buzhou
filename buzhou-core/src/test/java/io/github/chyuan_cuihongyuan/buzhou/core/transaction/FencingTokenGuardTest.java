package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.transaction.FencingTokenGuard.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5004 / T6110：Fencing 合同——易主拦截图景、未来 token
 * 拒、无锁拒、release 不重置、多锁独立、畸形 fail-fast。
 */
class FencingTokenGuardTest {

    @Test
    void currentTokenWriteShouldBeAccepted() {
        FencingTokenGuard guard = new FencingTokenGuard();
        long token = guard.acquire("lock", "owner-a");
        assertThat(token).isEqualTo(1L);
        assertThat(guard.tryWrite("lock", token)).isEqualTo(Verdict.ACCEPTED);
        assertThat(guard.holderOf("lock")).isEqualTo("owner-a");
    }

    @Test
    void staleHolderShouldBeRejectedAfterHandover() {
        FencingTokenGuard guard = new FencingTokenGuard();
        long staleToken = guard.acquire("lock", "owner-a");   // A 拿锁（随后停顿）
        long freshToken = guard.acquire("lock", "owner-b");   // 锁易主 → B 新代
        assertThat(staleToken).isEqualTo(1L);
        assertThat(freshToken).isEqualTo(2L);
        assertThat(guard.tryWrite("lock", staleToken)).isEqualTo(Verdict.STALE_TOKEN);   // A 苏醒写被拦
        assertThat(guard.tryWrite("lock", freshToken)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void unknownFutureTokenShouldBeRejected() {
        FencingTokenGuard guard = new FencingTokenGuard();
        guard.acquire("lock", "owner-a");
        assertThat(guard.tryWrite("lock", 99L)).isEqualTo(Verdict.UNKNOWN_TOKEN);
    }

    @Test
    void unwrittenLockShouldRejectWithNoLock() {
        FencingTokenGuard guard = new FencingTokenGuard();
        assertThat(guard.tryWrite("ghost", 1L)).isEqualTo(Verdict.NO_LOCK);
        assertThat(guard.currentToken("ghost")).isZero();
        assertThat(guard.holderOf("ghost")).isNull();
    }

    @Test
    void releaseShouldNotResetGeneration() {
        FencingTokenGuard guard = new FencingTokenGuard();
        long first = guard.acquire("lock", "owner-a");
        guard.release("lock", "owner-a");
        assertThat(guard.holderOf("lock")).isNull();
        long second = guard.acquire("lock", "owner-b");
        assertThat(second).isEqualTo(first + 1L);   // 世代跨释放递增——旧令牌永不复用
        assertThat(guard.tryWrite("lock", first)).isEqualTo(Verdict.STALE_TOKEN);
        assertThat(guard.tryWrite("lock", second)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    void locksShouldBeIndependent() {
        FencingTokenGuard guard = new FencingTokenGuard();
        long lockAToken = guard.acquire("lock-a", "owner-a");
        long lockBToken = guard.acquire("lock-b", "owner-b");
        assertThat(lockAToken).isEqualTo(1L);
        assertThat(lockBToken).isEqualTo(1L);   // 各锁世代独立（同起点互不牵连）
        assertThat(guard.currentToken("lock-a")).isEqualTo(1L);
        assertThat(guard.tryWrite("lock-a", 5L)).isEqualTo(Verdict.UNKNOWN_TOKEN);   // lock-a 未发过 5
    }

    @Test
    void invalidIdsShouldFailFast() {
        FencingTokenGuard guard = new FencingTokenGuard();
        assertThatThrownBy(() -> guard.acquire(null, "h")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.acquire("", "h")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.acquire("lock", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.tryWrite(null, 1L)).isInstanceOf(IllegalArgumentException.class);
        guard.acquire("lock", "h");
        assertThatThrownBy(() -> guard.release("lock", "other"))
                .isInstanceOf(IllegalArgumentException.class);   // 非当前持有者
    }
}
