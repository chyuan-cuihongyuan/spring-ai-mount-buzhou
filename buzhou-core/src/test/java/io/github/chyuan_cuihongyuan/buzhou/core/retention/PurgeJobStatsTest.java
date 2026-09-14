package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1078 / impl 830：归档清理任务读面——清理轮次（purgeRounds）、
 * 累计产出（purgedTotal）、resetForTest 归零。
 */
class PurgeJobStatsTest {

    private BuzhouStores stores;
    private SessionArchiver archiver;
    private String sid;

    @BeforeEach
    void setUp() {
        ArchivePurgeJob.resetForTest();
        stores = Buzhou.inMemoryStores();
        sid = "purge-sess";
        stores.messageStore().append(sid, List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sid, 1, 0, Role.USER, "内容",
                List.of(), null, null, null, Map.of(), Instant.now())));
        archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue();
    }

    @Test
    void purgeRoundCountsRoundsAndTotal() {
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ZERO,
                Duration.ofHours(1), false);

        assertThat(job.purgeOnce()).isEqualTo(1);

        ArchivePurgeJob.PurgeJobStats stats = ArchivePurgeJob.stats();
        assertThat(stats.purgeRounds()).isEqualTo(1);
        assertThat(stats.purgedTotal()).isEqualTo(1);
        assertThat(stats.skippedLocked()).isZero();
    }

    @Test
    void emptyRoundAddsRoundsWithoutPurged() {
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ofHours(24),
                Duration.ofHours(1), false);

        assertThat(job.purgeOnce()).isZero(); // TTL 未到 → 0 清理

        ArchivePurgeJob.PurgeJobStats stats = ArchivePurgeJob.stats();
        assertThat(stats.purgeRounds()).isEqualTo(1);
        assertThat(stats.purgedTotal()).isZero();
    }

    @Test
    void resetForTestZeroesCounters() {
        ArchivePurgeJob job = new ArchivePurgeJob(archiver, Duration.ZERO,
                Duration.ofHours(1), false);
        job.purgeOnce();
        assertThat(ArchivePurgeJob.stats().purgeRounds()).isEqualTo(1);

        ArchivePurgeJob.resetForTest();

        ArchivePurgeJob.PurgeJobStats stats = ArchivePurgeJob.stats();
        assertThat(stats.purgeRounds()).isZero();
        assertThat(stats.purgedTotal()).isZero();
        assertThat(stats.skippedLocked()).isZero();
    }
}
