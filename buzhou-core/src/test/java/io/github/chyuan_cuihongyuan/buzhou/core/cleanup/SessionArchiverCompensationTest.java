package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 304 / impl-327：归档 saga 补偿回归——级联部分失败（补偿全成）净回原状 +
 * 归档键撤；补偿再失败即停止回退——归档键保留（唯一完整副本，人工介入）。
 */
class SessionArchiverCompensationTest {

    private static BuzhouMessage message(int turn, String text) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s-comp", turn, 0,
                Role.USER, text, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 删除即抛的观测槽（级联尾部失败注入）。 */
    static final class FailingDeleteObservabilityStore extends InMemoryObservabilityStore {
        @Override
        public void deleteSession(String sessionId) {
            throw new IllegalStateException("obs-store-down");
        }
    }

    /** 可控失败的摘要槽（级联中部失败 + 补偿失败注入）。 */
    static final class FlakySummaryStore extends InMemorySummaryStore {
        boolean failDelete;
        boolean failSave;

        @Override
        public void deleteSession(String sessionId) {
            if (failDelete) {
                throw new IllegalStateException("summary-store-down");
            }
            super.deleteSession(sessionId);
        }

        @Override
        public long save(String sessionId, StructuredSummary summary) {
            if (failSave) {
                throw new IllegalStateException("summary-save-down");
            }
            return super.save(sessionId, summary);
        }
    }

    private BuzhouStores seededStores(BuzhouStores stores, String sid) {
        stores.messageStore().append(sid, List.of(message(1, "问"), message(2, "再问")));
        stores.summaryStore().save(sid, new StructuredSummary(sid, 3,
                Map.of("intent", "补偿"), 42, Instant.now()));
        stores.sessionStateStore().put(sid,
                new StateEntry("quota.turns", "7", "core", 1, null, Instant.now()));
        return stores;
    }

    @Test
    void cascadePartialFailure_compensationFullySucceeds_netRollback() {
        BuzhouStores base = Buzhou.inMemoryStores();
        BuzhouStores stores = seededStores(new BuzhouStores(base.messageStore(),
                base.summaryStore(), base.sessionStateStore(), base.sessionLeaseStore(),
                new FailingDeleteObservabilityStore(), base.unitOfWork()), "s-comp-1");
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));

        assertThatThrownBy(() -> archiver.archive("s-comp-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("级联清理部分失败")
                .hasMessageContaining("observability-store");

        // 补偿全成：活数据三槽复原（净回原状）+ 归档键撤
        assertThat(stores.messageStore().load("s-comp-1")).hasSize(2);
        assertThat(stores.summaryStore().latest("s-comp-1")).isPresent();
        assertThat(stores.sessionStateStore().get("s-comp-1", "quota.turns")).isPresent();
        assertThat(archiver.archived()).as("补偿全成 → 归档键撤").isEmpty();
    }

    @Test
    void compensationFailureHaltsUnwind_archiveKeyPreserved() {
        BuzhouStores base = Buzhou.inMemoryStores();
        FlakySummaryStore summary = new FlakySummaryStore();
        BuzhouStores stores = seededStores(new BuzhouStores(base.messageStore(),
                summary, base.sessionStateStore(), base.sessionLeaseStore(),
                base.observabilityStore(), base.unitOfWork()), "s-comp-2");
        summary.failDelete = true; // 级联在 summary 槽失败（seed 完成后再注入）
        summary.failSave = true;   // 补偿写回 summary 再失败
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));

        assertThatThrownBy(() -> archiver.archive("s-comp-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("级联清理部分失败")
                .hasMessageContaining("summary-store");

        // 停止回退：归档键保留（唯一完整副本）；补偿已写回的消息在（补偿在 summary 处断）
        assertThat(archiver.archived()).as("补偿失败 → 归档键保留").containsExactly("s-comp-2");
        assertThat(stores.messageStore().load("s-comp-2")).isNotEmpty();
    }
}
