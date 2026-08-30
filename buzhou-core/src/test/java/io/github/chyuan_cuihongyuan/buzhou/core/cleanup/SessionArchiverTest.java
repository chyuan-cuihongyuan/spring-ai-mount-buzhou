package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 97 §B / T364：会话归档冷层红队——归档后三槽清空 + 归档键在册；restore 回放
 * 三槽原键原值 + 归档键删除；空会话不归档诚实 false；无归档 restore false；
 * archived() 清单。SessionCleaner 前置安全网（冷存档优先于硬删）。
 */
class SessionArchiverTest {

    private static BuzhouMessage message(int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s-arch", turn, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void archiveRemovesAndRestoreReplaysAllThreeStores() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionStateStore state = stores.sessionStateStore();
        String sid = "s-arch-1";
        stores.messageStore().append(sid, List.of(message(1, "问"), message(2, "再问")));
        stores.summaryStore().save(sid, new StructuredSummary(sid, 3,
                Map.of("intent", "调优"), 42, Instant.now()));
        state.put(sid, new StateEntry("quota.turns", "7", "core", 1, null, Instant.now()));

        SessionArchiver archiver = new SessionArchiver(stores,
                new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue();

        // 归档后：原会话三槽清空，归档键在册
        assertThat(stores.messageStore().load(sid)).isEmpty();
        assertThat(stores.summaryStore().latest(sid)).isEmpty();
        assertThat(state.getAll(sid)).isEmpty();
        assertThat(archiver.archived()).containsExactly(sid);

        // 回放：三槽原值回归 + 归档键删除
        assertThat(archiver.restore(sid)).isTrue();
        assertThat(stores.messageStore().load(sid)).hasSize(2);
        assertThat(stores.summaryStore().latest(sid).orElseThrow().sections())
                .containsEntry("intent", "调优");
        assertThat(state.get(sid, "quota.turns").orElseThrow().value()).isEqualTo("7");
        assertThat(archiver.archived()).isEmpty();
    }

    @Test
    void emptySessionNotArchivedAndUnknownRestoreFalse() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));

        assertThat(archiver.archive("empty-sess")).isFalse(); // 空会话诚实不动
        assertThat(archiver.restore("never-archived")).isFalse();
        assertThat(archiver.archived()).isEmpty();
    }

    @Test
    void archivedListingSortedAndIndependentPerSession() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        for (String sid : new String[]{"b-sess", "a-sess"}) {
            stores.messageStore().append(sid, List.of(message(1, "x")));
            archiver.archive(sid);
        }

        assertThat(archiver.archived()).containsExactly("a-sess", "b-sess"); // 字典序
        // 单独恢复 a 不影响 b
        archiver.restore("a-sess");
        assertThat(archiver.archived()).containsExactly("b-sess");
    }
}
