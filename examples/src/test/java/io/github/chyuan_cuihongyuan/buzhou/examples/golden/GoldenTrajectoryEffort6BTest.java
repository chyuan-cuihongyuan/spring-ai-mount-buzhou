package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.SessionIndexObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹扩充 B（spec 34 §C / T117 / impl-92）：export/import 往返续用、工具结果限幅
 * （模型侧可见截断标记）、索引生命周期含 DELETED 联动。
 */
class GoldenTrajectoryEffort6BTest {

    // ---- G10 导出/导入往返 ----

    /** 导出→JSON→导入（新 Id）→ 以导入 Id spawn 续聊（历史注入）。 */
    @Test
    void g10ExportImportRoundTripAndResume() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("原会话答复：不周山");
        model.enqueueText("续聊答复");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "g10-src");
        source.chat("这座山叫什么");
        stores.summaryStore().save("g10-src", new StructuredSummary(
                "g10-src", 1, Map.of("core", "导出摘要"), 10, Instant.now()));
        stores.sessionStateStore().put("g10-src", new StateEntry(
                "budget.used", "500", "g", 1, null, Instant.now()));
        source.close();

        String json = runtime.exportSession("g10-src").toJson();
        String importedId = runtime.importSession(SessionExport.fromJson(json), false);

        assertThat(importedId).isNotEqualTo("g10-src");
        assertThat(stores.messageStore().load(importedId))
                .allSatisfy(m -> assertThat(m.sessionId()).isEqualTo(importedId));
        assertThat(stores.summaryStore().latest(importedId)).isPresent();
        assertThat(stores.sessionStateStore().get(importedId, "budget.used")).isPresent();

        AgentSession resumed = runtime.spawn("app", "agent", importedId);
        assertThat(resumed.chat("续问")).isEqualTo("续聊答复");
        assertThat(model.seenPrompts.get(1).getContents().toString()).contains("不周山");
        resumed.close();
    }

    // ---- G11 工具结果限幅 ----

    /** 大结果工具 → 回喂模型的结果含截断标记与原始尺寸；豁免工具原样透传。 */
    @Test
    void g11ToolResultTruncatedForModel() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        ToolCallback bigTool = FunctionToolCallback.builder("g11_big",
                        (java.util.function.Function<Map<String, Object>, String>) input ->
                                "y".repeat(100_000))
                .description("返回超大结果")
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema("{}")
                .build();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(RuntimeConfig.defaults(),
                        RuntimeConfig.autoTools(List.of(bigTool))));

        model.enqueue(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "g11_big", "{}")))
                .build());
        model.enqueueText("已读大结果");
        AgentSession session = runtime.spawn("app", "agent", "g11");
        session.chat("查大结果");

        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anySatisfy(m -> {
                    assertThat(m).isInstanceOf(ToolResponseMessage.class);
                    // FunctionToolCallback 结果经 JSON 字符串包装（±引号）——断言关键标记而非精确尺寸
                    String data = ((ToolResponseMessage) m).getResponses().getFirst().responseData();
                    assertThat(data).contains("结果已截断：原始 1")
                            .contains("超出上限 20000。请细化查询或分页读取所需部分");
                });
        session.close();
    }

    // ---- G12 索引生命周期（含 DELETED 联动） ----

    /** spawn→chat→close→delete：ACTIVE→turnCount 累计→CLOSED→DELETED（默认列表不可见）。 */
    @Test
    void g12IndexLifecycleThroughDelete() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.merge(
                RuntimeConfig.defaults(),
                RuntimeConfig.assemblyCustomizers(List.of(SessionIndexObserver.wiring(index))),
                RuntimeConfig.cleanupContributors(List.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor.of(
                                "session-index", sid -> index.get(sid).ifPresent(info ->
                                        index.upsert(new SessionInfo(info.sessionId(), info.appId(),
                                                info.agentName(), SessionInfo.STATUS_DELETED,
                                                info.createdAtEpochMs(), info.lastActiveAtEpochMs(),
                                                info.turnCount(), info.tags()))))))));

        AgentSession session = runtime.spawn("app-g12", "ag", "g12");
        session.chat("q1");
        session.chat("q2");
        assertThat(index.get("g12")).hasValueSatisfying(i -> {
            assertThat(i.status()).isEqualTo(SessionInfo.STATUS_ACTIVE);
            assertThat(i.turnCount()).isEqualTo(2);
        });
        session.close();
        assertThat(index.get("g12"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_CLOSED));

        session.delete();
        assertThat(index.get("g12"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_DELETED));
        assertThat(index.list(SessionIndexQuery.defaults())).isEmpty(); // 默认列表排除审计行
        assertThat(index.list(new SessionIndexQuery(
                null, null, SessionInfo.STATUS_DELETED, null, null, 0, 10)))
                .singleElement().extracting(SessionInfo::sessionId).isEqualTo("g12");
    }
}
