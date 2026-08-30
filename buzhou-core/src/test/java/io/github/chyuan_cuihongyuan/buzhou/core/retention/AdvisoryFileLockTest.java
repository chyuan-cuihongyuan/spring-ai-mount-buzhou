package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 182 §B / T539：文件咨询锁红队——抢锁互斥（第二持有者 false）；仅持有
 * 者可释放（他者释放被拒）；陈旧判定按持有时刻 + ttl（未到期不陈旧）；强制
 * 回收；参数 fail-fast。借鉴：ShedLock（多实例单跑）。
 */
class AdvisoryFileLockTest {

    private static final Instant T0 = Instant.parse("2026-08-29T00:00:00Z");

    @Test
    void acquireIsMutualAndOnlyOwnerReleases(@TempDir Path dir) throws Exception {
        AdvisoryFileLock lock = new AdvisoryFileLock(dir.resolve("purge.lock"));
        assertThat(lock.tryAcquire("instance-a", T0)).isTrue();
        assertThat(lock.tryAcquire("instance-b", T0)).isFalse(); // 互斥
        assertThat(lock.owner()).contains("instance-a");

        assertThat(lock.release("instance-b")).isFalse(); // 他者释放被拒
        assertThat(lock.owner()).contains("instance-a");

        assertThat(lock.release("instance-a")).isTrue(); // 持有者释放
        assertThat(lock.owner()).isEmpty();
        assertThat(lock.tryAcquire("instance-b", T0)).isTrue(); // 释放后可再抢
    }

    @Test
    void stalenessFollowsAcquiredAtAndTtl(@TempDir Path dir) throws Exception {
        AdvisoryFileLock lock = new AdvisoryFileLock(dir.resolve("job.lock"));
        lock.tryAcquire("instance-a", T0);

        assertThat(lock.stale(T0.plus(Duration.ofMinutes(9)), Duration.ofMinutes(10)))
                .isEmpty(); // 未到期
        assertThat(lock.stale(T0.plus(Duration.ofMinutes(11)), Duration.ofMinutes(10)))
                .contains("instance-a"); // 僵死残留可回收

        assertThat(lock.forceRelease()).isTrue(); // 处置动作
        assertThat(lock.owner()).isEmpty();
        assertThat(lock.forceRelease()).isFalse(); // 幂等
    }

    @Test
    void releaseOnMissingLockIsFalseAndArgsValidated(@TempDir Path dir) throws Exception {
        AdvisoryFileLock lock = new AdvisoryFileLock(dir.resolve("none.lock"));
        assertThat(lock.release("x")).isFalse();
        assertThat(lock.owner()).isEmpty();

        assertThatThrownBy(() -> lock.tryAcquire(" ", T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.tryAcquire("bad\nowner", T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.tryAcquire("ok", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.stale(T0, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
