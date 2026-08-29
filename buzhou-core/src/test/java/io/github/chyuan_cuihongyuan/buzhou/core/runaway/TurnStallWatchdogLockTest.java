package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.retention.AdvisoryFileLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 186 §B / T546：巡检犬接锁红队——锁被他持时 inspectOnce 返回 null、
 * <b>零通知</b>（空表语义保留给「真巡检过没事」）+ skippedForLock 计数；
 * 锁空闲时正常巡检且用后即还。
 */
class TurnStallWatchdogLockTest {

    @Test
    void skipsSilentlyWhenLockedAndInspectsWhenFree(@TempDir Path dir) throws Exception {
        AdvisoryFileLock lock = new AdvisoryFileLock(dir.resolve("watchdog.lock"));
        TurnHeartbeat heartbeat = new TurnHeartbeat();
        heartbeat.register("stuck", Instant.now().minusSeconds(3600));
        TurnStallWatchdog watchdog = new TurnStallWatchdog(heartbeat,
                Duration.ofSeconds(1), Duration.ofHours(1), false, lock);
        CopyOnWriteArrayList<List<TurnHeartbeat.Stalled>> reports = new CopyOnWriteArrayList<>();
        watchdog.addListener(reports::add);

        assertThat(lock.tryAcquire("other", Instant.now())).isTrue();
        assertThat(watchdog.inspectOnce()).isNull(); // 跳过：null 与空表区分
        assertThat(reports).isEmpty(); // 零通知（不是「没事」是「没跑」）
        assertThat(watchdog.skippedForLock()).isEqualTo(1);

        assertThat(lock.release("other")).isTrue();
        List<TurnHeartbeat.Stalled> stalled = watchdog.inspectOnce();
        assertThat(stalled).hasSize(1);
        assertThat(reports).hasSize(1);
        assertThat(watchdog.skippedForLock()).isEqualTo(1); // 不再累计

        List<TurnHeartbeat.Stalled> again = watchdog.inspectOnce(); // 用后即还
        assertThat(again).hasSize(1);
        assertThat(reports).hasSize(2);
    }
}
