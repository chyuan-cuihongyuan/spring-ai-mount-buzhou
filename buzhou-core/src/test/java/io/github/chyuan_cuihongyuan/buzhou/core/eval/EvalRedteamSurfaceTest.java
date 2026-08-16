package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 52 对抗面 / T196 / impl-162：评估面红队——注入伪造/键结构注入/记录篡改/会话隔离。
 */
class EvalRedteamSurfaceTest {

    /** 注入①：伪造超范围 turnSeq 负反馈（直接写 state）→ 不入集不崩（skippedMissingReply 挡）。 */
    @Test
    void forgedOutOfRangeNegativeFeedbackDoesNotEnterDataset() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d", null);

        // 伪造：会话只有 1 轮，state 里塞 turnSeq=99 的负反馈
        try (var session = runtime.spawn("app", "agent", "victim")) {
            session.chat("hi"); // turn 1
        }
        stores.sessionStateStore().put("victim", new StateEntry(
                "buzhou.feedback.99.1-1", "type=boolean&value=false&source=user",
                "turn-feedback", 99, null, Instant.now()));

        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);
        var result = importer.importFromFeedback("victim", "d");
        assertThat(result.imported()).isZero();
        assertThat(result.skippedMissingReply()).isEqualTo(1);
        assertThat(datasetStore.items("d")).isEmpty();
    }

    /** 注入②：dataset 名键结构注入（含点/斜杠/空格）→ NAME_PATTERN 拒绝（键布局不可逃逸）。 */
    @Test
    void datasetNameKeyStructureInjectionRejected() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        for (String bad : new String[]{"eval.ds.x", "a/b", "a b", "a:b", ".hidden"}) {
            assertThatThrownBy(() -> datasetStore.createDataset(bad, null))
                    .isInstanceOf(BuzhouException.class)
                    .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                            .isEqualTo(ErrorCode.EVAL_OPERATION_INVALID));
        }
        assertThat(datasetStore.listDatasets()).isEmpty();
    }

    /** 篡改：run 记录 JSON 损坏/被改写 → 查询面 DATA_CORRUPTION 快速失败（不静默半解析）。 */
    @Test
    void tamperedRunRecordFailsFastAsDataCorruption() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d", null);
        datasetStore.addItem("d", "q", "a", null, null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        String runId = runner.run("d", BuiltInEvaluators.EXACT).runId();

        // 篡改 run 记录为非法 JSON
        stores.sessionStateStore().put(EvalDatasetStore.SESSION_ID, new StateEntry(
                EvalRunner.RUN_PREFIX + runId, "{not-json", "attacker", 0, null, Instant.now()));

        EvalQueryService query = new EvalQueryService(stores.sessionStateStore());
        assertThatThrownBy(() -> query.run(runId))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.DATA_CORRUPTION));
    }

    /** 隔离：业务会话与评估 run 并存互不干扰（命名空间独立 + 互不串数据）。 */
    @Test
    void evalSessionsDoNotPolluteBusinessSessions() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("biz-reply");
        model.enqueueText("a");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d", null);
        datasetStore.addItem("d", "q", "a", null, null);

        // 业务会话先开启（在途状态），评估 run 并行其后再回到业务会话续聊
        var biz = runtime.spawn("app", "agent", "biz-session");
        assertThat(biz.chat("业务问题")).isEqualTo("biz-reply");
        new EvalRunner(runtime, datasetStore, stores.sessionStateStore())
                .run("d", BuiltInEvaluators.EXACT);
        // 业务会话可继续（未被评估面抢占/关闭）；消息历史无评估内容混入
        model.enqueueText("biz-reply-2");
        assertThat(biz.chat("继续")).isEqualTo("biz-reply-2");
        assertThat(stores.messageStore().load("biz-session"))
                .allSatisfy(m -> assertThat(m.content()).doesNotContainIgnoringCase("eval"));
        biz.close();
    }
}
