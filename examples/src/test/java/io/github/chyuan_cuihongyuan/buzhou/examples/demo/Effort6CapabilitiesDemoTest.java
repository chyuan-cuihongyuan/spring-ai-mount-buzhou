package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsck;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreIntegrityReport;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardQueryService;
import io.github.chyuan_cuihongyuan.buzhou.spill.MediaIntake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #6/#7 新能力演示（T126 / impl-101）：MediaIntake 字节→多模态闭环、导出/导入
 * 备份恢复、fsck 报告、索引 + dashboard 过滤列表、限幅豁免——examples 接缝文档。
 */
class Effort6CapabilitiesDemoTest {

    /** 1) MediaIntake：上传字节 → spill URI → chat(input, media) 随轮下发。 */
    @Test
    void mediaIntakeFeedsMultimodalChat(@TempDir Path spillRoot) {
        MediaIntake intake = new MediaIntake(
                new io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore(spillRoot));
        MediaRef screenshot = intake.intakeText("图表数据：Q1 120 Q2 180 Q3 240",
                "text/markdown", "demo-agent", "media-demo");

        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("图表显示增长趋势");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "demo-agent", "media-demo");

        assertThat(session.chat("看这份数据", List.of(screenshot))).contains("增长");
        assertThat(model.seenPrompts.getFirst().getInstructions().stream()
                .filter(m -> m instanceof org.springframework.ai.chat.messages.UserMessage)
                .map(m -> (org.springframework.ai.chat.messages.UserMessage) m)
                .findFirst().orElseThrow().getMedia()).hasSize(1);
        // 字节回读无损（备份/审计通道）
        assertThat(intake.readBackText(screenshot)).contains("Q3 240");
        session.close();
    }

    /** 2) 备份恢复：导出→close 删除源→导入新 Id→续聊（灾难恢复演示）。 */
    @Test
    void exportImportAsBackupRestore() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("原始答复");
        model.enqueueText("恢复后续聊");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "ag", "backup-src");
        source.chat("关键业务问题");
        stores.summaryStore().save("backup-src", new StructuredSummary(
                "backup-src", 1, Map.of("core", "业务上下文"), 8, Instant.now()));
        String backupJson = runtime.exportSession("backup-src").toJson();
        source.delete(); // 源会话湮灭

        String restoredId = runtime.importSession(SessionExport.fromJson(backupJson), false);
        AgentSession restored = runtime.spawn("app", "ag", restoredId);
        assertThat(restored.chat("恢复后继续")).isEqualTo("恢复后续聊");
        // 历史（含源问答）注入恢复会话——摘要数据随档迁移（注入渲染需 memory 模块，不在默认 runtime 面）
        assertThat(model.seenPrompts.get(1).getContents().toString())
                .contains("关键业务问题").contains("原始答复");
        restored.close();
    }

    /** 3) fsck：孤儿摘要检测 + 人读报告 + 选择性修复。 */
    @Test
    void fsckReportsAndRepairsOrphanSummary() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.messageStore().append("healthy", List.of(new BuzhouMessage(
                "m-1", "healthy", 1, 1, Role.USER, "q", List.of(), null, null, null,
                Map.of(), Instant.now())));
        stores.observabilityStore().saveSpans(List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord(
                "sp-1", null, "healthy", 1, "SESSION", "session", Instant.now(), Instant.now(),
                "OK", Map.of())));
        stores.summaryStore().save("orphan", new StructuredSummary(
                "orphan", 1, Map.of(), 1, Instant.now())); // 无消息也无观测 → extras 补充进全集
        stores.observabilityStore().saveSpans(List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord(
                "sp-2", null, "orphan", 1, "SESSION", "session", Instant.now(), Instant.now(),
                "OK", Map.of())));

        StoreIntegrityReport report = StoreFsck.run(stores);
        assertThat(report.renderText()).contains(StoreIntegrityReport.ORPHAN_SUMMARY);
        StoreFsck.repair(stores, report, new StoreFsck.RepairOptions(true, false, false));
        assertThat(StoreFsck.run(stores).count(StoreIntegrityReport.ORPHAN_SUMMARY)).isZero();
    }

    /** 4) 索引 + dashboard：会话活跃排行与过滤列表（运维排障入口）。 */
    @Test
    void indexBacksDashboardFilteredListing() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(new SessionInfo("hot", "prod-app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 3000L, 42, Map.of("env", "prod")));
        index.upsert(new SessionInfo("cold", "prod-app", "ag", SessionInfo.STATUS_CLOSED,
                1L, 1000L, 3, Map.of("env", "prod")));
        index.upsert(new SessionInfo("other-env", "prod-app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 2000L, 7, Map.of("env", "staging")));

        DashboardQueryService service = new DashboardQueryService(
                Buzhou.inMemoryStores().observabilityStore(), index);

        DashboardQueryService.IndexedSessionPage prod =
                service.listSessionsFiltered("prod-app", null, null, "env", "prod", null, 10);
        assertThat(prod.fromIndex()).isTrue();
        assertThat(prod.items()).extracting(SessionInfo::sessionId)
                .containsExactly("hot", "cold"); // lastActive 倒序

        DashboardQueryService.IndexedSessionPage active =
                service.listSessionsFiltered("prod-app", null, SessionInfo.STATUS_ACTIVE,
                        null, null, null, 10);
        assertThat(active.items()).extracting(SessionInfo::sessionId)
                .containsExactly("hot", "other-env"); // 两个 ACTIVE 按 lastActive 倒序
    }

    /** 5) 导出扩展段：facts 随文档跨环境迁移（state 命名空间无损）。 */
    @Test
    void factsTravelWithExportExtensions() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.sessionStateStore().put("facts-src", new StateEntry(
                "fact.demo.key-1", "{\"v\":1}", "demo-hook", 2, 100, Instant.now()));
        stores.sessionStateStore().put("facts-src", new StateEntry(
                "other.key", "x", "core", 1, null, Instant.now())); // 非 fact 段
        io.github.chyuan_cuihongyuan.buzhou.memory.FactsExporter factsExporter =
                new io.github.chyuan_cuihongyuan.buzhou.memory.FactsExporter(stores.sessionStateStore());
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        ((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtime)
                .setExportExtensions(List.of(factsExporter));
        AgentSession source = runtime.spawn("app", "ag", "facts-src");
        source.chat("q");
        source.close();

        String json = runtime.exportSession("facts-src").toJson();
        String importedId = runtime.importSession(SessionExport.fromJson(json), false);

        // 扩展段只含 fact.*（other.key 属 state 全槽——三槽导出面本就携带，与扩展段正交）
        SessionExport parsed = SessionExport.fromJson(json);
        assertThat(parsed.extensions()).containsKey("memory.facts");
        assertThat(parsed.extensions().get("memory.facts")).contains("fact.demo.key-1")
                .doesNotContain("other.key");
        assertThat(stores.sessionStateStore().get(importedId, "fact.demo.key-1")).isPresent();
    }
}
