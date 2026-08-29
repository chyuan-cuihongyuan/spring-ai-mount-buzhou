package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 102 §B / T380：会话归档健康面红队——恒 UP；无归档 0（合法）；归档数随
 * archive/restore 走；端点聚合段。spec 97 fog 收口。
 */
class ArchiveHealthTest {

    @Test
    void alwaysUpCountTracksArchiveLifecycle() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        ArchiveHealth health = new ArchiveHealth(stores.sessionStateStore());

        assertThat(health.mechanism()).isEqualTo("session-archive");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("archivedSessions", 0); // 空合法

        String sid = "s-health";
        stores.messageStore().append(sid, List.of(new BuzhouMessage(UUID.randomUUID().toString(),
                sid, 1, 0, Role.USER, "x", List.of(), null, null, null, Map.of(), Instant.now())));
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        archiver.archive(sid);

        assertThat(health.details()).containsEntry("archivedSessions", 1);
        archiver.restore(sid);
        assertThat(health.details()).containsEntry("archivedSessions", 0);
    }

    @Test
    void endpointAggregatesArchiveSection() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(
                List.of(new ArchiveHealth(stores.sessionStateStore())));

        Map<String, Object> snapshot = endpoint.buzhouSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> mechanisms = (Map<String, Object>) snapshot.get("mechanisms");
        assertThat(mechanisms).containsKey("session-archive");
    }
}
