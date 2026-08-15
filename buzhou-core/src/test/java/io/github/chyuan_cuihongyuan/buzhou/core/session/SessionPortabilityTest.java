package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话可移植导出/导入 e2e（spec 28 / T107 / impl-82）：JSON 往返保真、默认 Id 重映射、
 * keepIds 冲突 fail-fast、导入后续用（spawn 同 Id 续聊）、spill 引用清单、空源/空档拒绝。
 */
class SessionPortabilityTest {

    /** 导出 → JSON → 导入（新 Id 重映射）→ 数据保真 + 以新 Id 续聊。 */
    @Test
    void exportJsonImportRoundTripWithIdRemap() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("答案一");
        model.enqueueText("续聊答案");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "port-src");
        source.chat("问题一");
        stores.summaryStore().save("port-src", new StructuredSummary(
                "port-src", 1, Map.of("core", "摘要内容"), 42, Instant.now()));
        stores.sessionStateStore().put("port-src", new StateEntry(
                "budget.used", "1000", "test", 1, null, Instant.now()));

        // 活跃会话导出（appId/agentName 尽力携带）；close 后导出则两者为 null（文档口径）
        SessionExport export = runtime.exportSession("port-src");
        assertThat(export.appId()).isEqualTo("app");
        assertThat(export.agentName()).isEqualTo("agent");
        assertThat(export.spillRefs()).isEmpty();

        String json = export.toJson();
        SessionExport parsed = SessionExport.fromJson(json);
        source.close();
        String importedId = runtime.importSession(parsed, false);

        assertThat(importedId).isNotEqualTo("port-src");
        assertThat(stores.messageStore().load(importedId))
                .allSatisfy(m -> assertThat(m.sessionId()).isEqualTo(importedId))
                .hasSize(2);
        assertThat(stores.summaryStore().latest(importedId))
                .hasValueSatisfying(sum -> assertThat(sum.sections()).containsEntry("core", "摘要内容"));
        assertThat(stores.sessionStateStore().get(importedId, "budget.used"))
                .hasValueSatisfying(entry -> assertThat(entry.value()).isEqualTo("1000"));

        // 导入会话可续用：以导入 Id spawn 续聊（历史注入）
        AgentSession resumed = runtime.spawn("app", "agent", importedId);
        assertThat(resumed.chat("续问")).isEqualTo("续聊答案");
        assertThat(model.seenPrompts.get(1).getContents().toString()).contains("问题一");
        resumed.close();
    }

    /** keepIds：目标空闲时原 Id 落位；目标已存在消息时 fail-fast 不静默覆盖。 */
    @Test
    void keepIdsConflictFailsFast() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "keep-src");
        source.chat("q");
        source.close();
        SessionExport export = runtime.exportSession("keep-src");
        source.close();
        String json = export.toJson();

        // 跨环境导入（全新 stores 的第二 runtime）：空闲目标原 Id 落位
        ScriptedChatModel model2 = new ScriptedChatModel();
        AgentRuntime runtime2 = Buzhou.runtime(model2, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults());
        assertThat(runtime2.importSession(SessionExport.fromJson(json), true)).isEqualTo("keep-src");

        // 同环境再导入同 Id：冲突 fail-fast（绝不静默覆盖）
        assertThatThrownBy(() -> runtime2.importSession(SessionExport.fromJson(json), true))
                .isInstanceOf(SessionImportException.class)
                .hasMessageContaining("keep-src");
    }

    /** 空源拒绝；空档拒绝；格式/版本不符拒绝。 */
    @Test
    void rejectsEmptySourceAndInvalidDocuments() {
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model,
                Buzhou.inMemoryStores(), RuntimeConfig.defaults());

        assertThatThrownBy(() -> runtime.exportSession("no-such-session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无消息历史");

        assertThatThrownBy(() -> runtime.importSession(null, false))
                .isInstanceOf(SessionImportException.class);

        assertThatThrownBy(() -> SessionExport.fromJson("{\"format\":\"other\",\"version\":1}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非本格式");
    }

    /** 导出扩展段（spec 36 §A / T121）：模块段往返 + 未知段/失败段不阻断。 */
    @Test
    void exportExtensionsRoundTripAndTolerateFailures() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        java.util.List<String> importedSegments = new java.util.concurrent.CopyOnWriteArrayList<>();
        ((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtime)
                .setExportExtensions(java.util.List.of(
                        new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExportExtension() {
                            @Override
                            public String name() {
                                return "test.segment";
                            }

                            @Override
                            public String exportSegment(String sessionId) {
                                return "{\"hello\":\"" + sessionId + "\"}";
                            }

                            @Override
                            public void importSegment(String targetSessionId, String json) {
                                importedSegments.add(targetSessionId + ":" + json);
                            }
                        },
                        new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExportExtension() {
                            @Override
                            public String name() {
                                return "test.broken";
                            }

                            @Override
                            public String exportSegment(String sessionId) {
                                throw new IllegalStateException("导出炸了");
                            }

                            @Override
                            public void importSegment(String targetSessionId, String json) {
                                throw new IllegalStateException("导入炸了");
                            }
                        }));
        AgentSession source = runtime.spawn("app", "agent", "ext-src");
        source.chat("q");
        source.close();

        String json = runtime.exportSession("ext-src").toJson();
        // 失败段被跳过、正常段随文档走
        assertThat(json).contains("test.segment").doesNotContain("test.broken");

        String importedId = runtime.importSession(io.github.chyuan_cuihongyuan.buzhou.core.session
                .SessionExport.fromJson(json), false);
        assertThat(importedSegments).singleElement().asString()
                .contains("hello").contains(importedId);
    }

    /** spill 引用清单：metadata 带 spillUri 的消息进入清单（消费方感知证据面）。 */
    @Test
    void spillRefsDerivedFromMessageMetadata() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "spill-src");
        session.chat("带证据的问");
        stores.messageStore().append("spill-src", java.util.List.of(
                new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                        "ev-1", "spill-src", 2, 1,
                        io.github.chyuan_cuihongyuan.buzhou.core.message.Role.TOOL,
                        "[旧工具结果已清理]", java.util.List.of(), "call-1", null, null,
                        Map.of("toolName", "run_query", "spillUri", "spill://agent/s1/t1"),
                        Instant.now())));
        session.close();

        SessionExport export = runtime.exportSession("spill-src");
        assertThat(export.spillRefs()).containsExactly(
                Map.of("evidenceId", "ev-1", "spillUri", "spill://agent/s1/t1"));

        // JSON 往返后 metadata（含 spillUri）保真
        SessionExport parsed = SessionExport.fromJson(export.toJson());
        assertThat(parsed.messages().stream()
                .filter(m -> "ev-1".equals(m.id()))
                .findFirst().orElseThrow().metadata())
                .containsEntry("spillUri", "spill://agent/s1/t1");
    }
}
