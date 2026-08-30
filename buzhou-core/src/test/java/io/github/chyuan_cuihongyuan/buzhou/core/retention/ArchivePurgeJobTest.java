package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 127 §B / T455：归档定时清理红队——默认关不排程但 purgeOnce 手动面可用；
 * ttl≤0 显式全清语义透传（到期删除委托 purgeExpired）；开启后周期触发（listener
 * 可观测不静默，未到期每轮 0 也通知）且 stop 后不再触发；参数 fail-fast。
 * 借鉴：S3 lifecycle 触发 + RetentionSweeper 同骨架（SmartLifecycle 自调度可关）。
 */
class ArchivePurgeJobTest {

    private static BuzhouMessage message(String sid, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sid, 1, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private SessionArchiver archiverWithOneArchive() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "s-purge-1";
        stores.messageStore().append(sid, List.of(message(sid, "归档前的问句")));
        stores.sessionStateStore().put(sid, new StateEntry("quota.turns", "3", "core", 1, null,
                Instant.now()));
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue();
        return archiver;
    }

    @Test
    void disabledNeverSchedulesButManualPurgeWorks() {
        SessionArchiver archiver = archiverWithOneArchive();
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ZERO,
                Duration.ofHours(1), false);
        AtomicInteger notified = new AtomicInteger(-1);
        job.addPurgeListener(notified::set);

        job.start();
        assertThat(job.isRunning()).isFalse(); // 默认关：不排程

        // 手动面：ttl=0 显式全清（spec 103 语义透传），listener 通知删除数
        assertThat(job.purgeOnce()).isEqualTo(1);
        assertThat(notified).hasValue(1);
        assertThat(archiver.archived()).isEmpty();
        job.stop();
    }

    @Test
    void enabledSchedulesPeriodicallyAndStopHalts() throws Exception {
        SessionArchiver archiver = archiverWithOneArchive();
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ofHours(24),
                Duration.ofMillis(40), true);
        CopyOnWriteArrayList<Integer> seen = new CopyOnWriteArrayList<>();
        job.addPurgeListener(seen::add);

        job.start();
        assertThat(job.isRunning()).isTrue();
        // ttl=24h：归档未到期——每轮删除 0（「跑过但无事可做」是事实，0 也通知）
        TimeUnit.MILLISECONDS.sleep(300);
        job.stop();
        assertThat(job.isRunning()).isFalse();
        assertThat(seen.size()).isGreaterThanOrEqualTo(2);
        assertThat(seen).containsOnly(0);
        assertThat(archiver.archived()).hasSize(1); // 未到期不删

        int sizeAtStop = seen.size();
        TimeUnit.MILLISECONDS.sleep(150);
        assertThat(seen.size()).isEqualTo(sizeAtStop); // stop 后不再触发
    }

    @Test
    void constructorValidatesFailFast() {
        SessionArchiver archiver = archiverWithOneArchive();
        assertThatThrownBy(() -> new ArchivePurgeJob(archiver, null, Duration.ofHours(1), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArchivePurgeJob(archiver, Duration.ofDays(1), null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArchivePurgeJob(archiver, Duration.ofDays(1),
                Duration.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class);
        // enabled=false 时 interval 不校验（不排程就不生效）
        new ArchivePurgeJob(archiver, Duration.ofDays(1), Duration.ZERO, false).close();
    }
}
