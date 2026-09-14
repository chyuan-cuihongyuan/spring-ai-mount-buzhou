package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
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
 * spec 1075 / impl 827：会话归档操作读面——成功归档（archived）、
 * 空会话跳过（emptySkipped）、resetForTest 归零。
 */
class ArchiveStatsTest {

    @BeforeEach
    void reset() {
        SessionArchiver.resetForTest();
    }

    @Test
    void successfulArchiveCountsArchived() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "arch-sess";
        stores.messageStore().append(sid, List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sid, 1, 0, Role.USER, "内容",
                List.of(), null, null, null, Map.of(), Instant.now())));
        SessionArchiver archiver = new SessionArchiver(stores, null);

        assertThat(archiver.archive(sid)).isTrue();

        SessionArchiver.ArchiveStats stats = SessionArchiver.stats();
        assertThat(stats.archiveCalls()).isEqualTo(1);
        assertThat(stats.archived()).isEqualTo(1);
        assertThat(stats.emptySkipped()).isZero();
    }

    @Test
    void emptySessionCountsSkipped() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, null);

        assertThat(archiver.archive("empty-sess")).isFalse();

        SessionArchiver.ArchiveStats stats = SessionArchiver.stats();
        assertThat(stats.emptySkipped()).isEqualTo(1);
        assertThat(stats.archived()).isZero();
    }

    @Test
    void resetForTestZeroesCounters() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, null);
        archiver.archive("empty-sess");
        assertThat(SessionArchiver.stats().archiveCalls()).isEqualTo(1);

        SessionArchiver.resetForTest();

        SessionArchiver.ArchiveStats stats = SessionArchiver.stats();
        assertThat(stats.archiveCalls()).isZero();
        assertThat(stats.emptySkipped()).isZero();
        assertThat(stats.archived()).isZero();
    }
}
