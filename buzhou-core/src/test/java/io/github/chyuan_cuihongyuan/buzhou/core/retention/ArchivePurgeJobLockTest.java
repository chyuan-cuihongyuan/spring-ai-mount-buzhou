package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 184 §B / T542：归档清理接锁红队——锁被他持时 purgeOnce 跳过
 * （SKIPPED_LOCKED=-1 + archiver 零调用 + listener 收 -1）；锁空闲时正常
 * 清理且用后即还（下一轮可再抢）；无锁构造零变化（既有回归覆盖）。
 */
class ArchivePurgeJobLockTest {

    private SessionArchiver archiverWithOneArchive() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "s-lock-1";
        stores.messageStore().append(sid, List.of(new io.github.chyuan_cuihongyuan
                .buzhou.core.message.BuzhouMessage(java.util.UUID.randomUUID().toString(),
                sid, 1, 0, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                "归档前", List.of(), null, null, null, java.util.Map.of(), Instant.now())));
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue();
        return archiver;
    }

    @Test
    void skipsWhenLockHeldByOtherAndPurgesWhenFree(@TempDir Path dir) throws Exception {
        AdvisoryFileLock lock = new AdvisoryFileLock(dir.resolve("purge.lock"));
        SessionArchiver archiver = archiverWithOneArchive();
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ZERO,
                Duration.ofHours(1), false, lock);
        AtomicInteger notified = new AtomicInteger(99);
        job.addPurgeListener(notified::set);

        assertThat(lock.tryAcquire("other-instance", Instant.now())).isTrue();
        assertThat(job.purgeOnce()).isEqualTo(ArchivePurgeJob.SKIPPED_LOCKED);
        assertThat(notified).hasValue(-1); // 「别的实例在跑」也是事实
        assertThat(archiver.archived()).hasSize(1); // archiver 零调用

        assertThat(lock.release("other-instance")).isTrue();
        assertThat(job.purgeOnce()).isEqualTo(1); // 正常清理
        assertThat(notified).hasValue(1);
        assertThat(archiver.archived()).isEmpty();

        assertThat(job.purgeOnce()).isZero(); // 用后即还：下一轮可再抢（空转 0）
        assertThat(notified).hasValue(0);
    }
}
