package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 103 §B / T382：归档 TTL 清理红队——过期删/新鲜留；ttl≤0 全清；空批次零操作；
 * 损坏归档跳过不阻断。spec 102 fog 后半场（冷层不是永久层——合规期后让位容量）。
 */
class SessionArchivePurgeTest {

    private static void archiveOne(BuzhouStores stores, SessionArchiver archiver, String sid) {
        stores.messageStore().append(sid, List.of(new BuzhouMessage(UUID.randomUUID().toString(),
                sid, 1, 0, Role.USER, "x", List.of(), null, null, null, Map.of(), Instant.now())));
        archiver.archive(sid);
    }

    @Test
    void purgeRemovesExpiredAndKeepsFresh() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        archiveOne(stores, archiver, "old-sess");
        archiveOne(stores, archiver, "new-sess");

        // now = 未来 30 天，ttl 7 天 → old/new 都过期？都刚归档——用两个不同 now 模拟：
        // 直接用当前时间 + ttl 7d：两条都新鲜（不删）
        int none = archiver.purgeExpired(Duration.ofDays(7), Instant.now());
        assertThat(none).isZero();
        assertThat(archiver.archived()).hasSize(2);

        // 时间快进 8 天：两条都过期
        int all = archiver.purgeExpired(Duration.ofDays(7), Instant.now().plus(Duration.ofDays(8)));
        assertThat(all).isEqualTo(2);
        assertThat(archiver.archived()).isEmpty();
    }

    @Test
    void nonPositiveTtlMeansExplicitPurgeAll() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        archiveOne(stores, archiver, "a");
        archiveOne(stores, archiver, "b");

        assertThat(archiver.purgeExpired(Duration.ZERO, Instant.now())).isEqualTo(2);
        assertThat(archiver.archived()).isEmpty();
        assertThat(archiver.purgeExpired(Duration.ZERO, Instant.now())).isZero(); // 幂等空批
    }

    @Test
    void corruptedArchiveEntrySkippedNotFatal() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        archiveOne(stores, archiver, "good");
        // 手工铺一条损坏归档（非 JSON）
        stores.sessionStateStore().put(SessionArchiver.ARCHIVE_SESSION_ID,
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                        SessionArchiver.ARCHIVE_PREFIX + "bad", "not-json",
                        "session-archiver", 0, null, Instant.now()));

        int purged = archiver.purgeExpired(Duration.ZERO, Instant.now());

        assertThat(purged).isEqualTo(1); // 好的删了
        // 坏的跳过（仍留——修复走手工删）
        assertThat(stores.sessionStateStore().get(SessionArchiver.ARCHIVE_SESSION_ID,
                SessionArchiver.ARCHIVE_PREFIX + "bad")).isPresent();
    }
}
