package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Store fsck 测试（spec 29 / T108 / impl-83）：四检测项对账 + 修复选择性清除 +
 * 观测只报不清 + 干净库零发现。会话全集 = 观测留痕 + extras 补充。
 */
class StoreFsckTest {

    private static final Instant NOW = Instant.now();

    /** 干净库（有消息有观测）：零发现。 */
    @Test
    void cleanStoresReportNothing() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedMessages(stores, "healthy");
        seedSpan(stores, "healthy");

        StoreIntegrityReport report = StoreFsck.run(stores);
        assertThat(report.clean()).isTrue();
        assertThat(report.renderText()).contains("CLEAN");
    }

    /** 四检测项：孤儿摘要/残留 state/泄漏租约/悬挂观测 各自命中且互不误报。 */
    @Test
    void detectsAllFourAnomalies() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        // healthy：消息+观测齐全
        seedMessages(stores, "healthy");
        seedSpan(stores, "healthy");
        // 孤儿摘要：摘要+观测、无消息
        stores.summaryStore().save("orphan", new StructuredSummary(
                "orphan", 1, Map.of(), 1, NOW));
        seedSpan(stores, "orphan");
        // 残留 state：state+观测、无消息无摘要
        stores.sessionStateStore().put("residue", new StateEntry("k", "v", "t", 1, null, NOW));
        seedSpan(stores, "residue");
        // 泄漏租约：租约+消息（有消息但租约在——消息存在不构成泄漏？此处构造无消息租约）
        stores.sessionLeaseStore().tryAcquire("leaked", "owner", java.time.Duration.ofMinutes(5));
        seedSpan(stores, "leaked");
        // 悬挂观测：仅观测
        seedSpan(stores, "ghost");

        StoreIntegrityReport report = StoreFsck.run(stores);
        assertThat(report.count(StoreIntegrityReport.ORPHAN_SUMMARY)).isEqualTo(1);
        assertThat(report.count(StoreIntegrityReport.STATE_RESIDUE)).isEqualTo(1);
        assertThat(report.count(StoreIntegrityReport.DANGLING_LEASE)).isEqualTo(1);
        // ghost（仅观测）+ leaked（仅租约+观测，数据面全空）都命中悬挂观测
        assertThat(report.count(StoreIntegrityReport.DANGLING_OBSERVABILITY)).isEqualTo(2);
        assertThat(report.samples(StoreIntegrityReport.ORPHAN_SUMMARY))
                .singleElement().extracting(f -> f.sessionId()).isEqualTo("orphan");
    }

    /** extras 补充全集：观测未留痕的会话也可校验。 */
    @Test
    void extraSessionIdsExtendUniverse() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.summaryStore().save("no-obs", new StructuredSummary(
                "no-obs", 1, Map.of(), 1, NOW));

        StoreIntegrityReport report = StoreFsck.run(stores, java.util.Set.of("no-obs"));
        assertThat(report.count(StoreIntegrityReport.ORPHAN_SUMMARY)).isEqualTo(1);
    }

    /** 修复选择性清除：勾选项清掉、未勾选项保留、悬挂观测永不自动清。 */
    @Test
    void repairHonorsOptionsAndNeverCleansObservability() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.summaryStore().save("orphan", new StructuredSummary(
                "orphan", 1, Map.of(), 1, NOW));
        seedSpan(stores, "orphan");
        stores.sessionStateStore().put("residue", new StateEntry("k", "v", "t", 1, null, NOW));
        seedSpan(stores, "residue");
        stores.sessionLeaseStore().tryAcquire("leaked", "owner", java.time.Duration.ofMinutes(5));
        seedSpan(stores, "leaked");

        StoreIntegrityReport report = StoreFsck.run(stores);
        // 只清孤儿摘要
        var repaired = StoreFsck.repair(stores, report,
                new StoreFsck.RepairOptions(true, false, false));
        assertThat(repaired).containsEntry(StoreIntegrityReport.ORPHAN_SUMMARY, 1);
        assertThat(stores.summaryStore().latest("orphan")).isEmpty();
        assertThat(stores.sessionStateStore().getAll("residue")).isNotEmpty(); // 保留
        assertThat(stores.sessionLeaseStore().inspect("leaked")).isPresent(); // 保留

        // 全清（观测除外）
        var repairedAll = StoreFsck.repair(stores, report,
                new StoreFsck.RepairOptions(false, true, true));
        assertThat(repairedAll).containsEntry(StoreIntegrityReport.STATE_RESIDUE, 1)
                .containsEntry(StoreIntegrityReport.DANGLING_LEASE, 1);
        assertThat(stores.sessionStateStore().getAll("residue")).isEmpty();
        assertThat(stores.sessionLeaseStore().inspect("leaked")).isEmpty();
        // 观测留痕仍在（永不自动清）
        assertThat(stores.observabilityStore().spansOfSession("ghost-or-orphan")).isNotNull();
        assertThat(stores.observabilityStore().listSessionSummaries(null, 100))
                .extracting(io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary::sessionId)
                .contains("orphan", "residue", "leaked");
    }

    // ---- helpers ----

    private static void seedMessages(BuzhouStores stores, String sessionId) {
        stores.messageStore().append(sessionId, List.of(new BuzhouMessage(
                "m-" + sessionId, sessionId, 1, 1, Role.USER, "q", List.of(), null,
                null, null, Map.of(), NOW)));
    }

    private static void seedSpan(BuzhouStores stores, String sessionId) {
        stores.observabilityStore().saveSpans(List.of(new SpanRecord(
                "span-" + sessionId, null, sessionId, 1, "SESSION", "session", NOW, NOW,
                "OK", Map.of())));
    }
}
