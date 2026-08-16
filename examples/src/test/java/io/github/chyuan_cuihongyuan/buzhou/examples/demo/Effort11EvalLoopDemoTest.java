package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.BuiltInEvaluators;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalQueryService;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalRunner;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.FeedbackImporter;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #11 评估闭环演示（spec 52 / T198 / impl-164）：负反馈产生 → 回流建数据集 →
 * run 执行 → 汇总查询 → 事件到达——「宿主视角完整闭环」的可运行样例。
 */
class Effort11EvalLoopDemoTest {

    @Test
    void demoFullEvalLoopFromNegativeFeedbackToQueryAndEvent() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = (DefaultAgentRuntime) Buzhou.runtime(
                model, stores, RuntimeConfig.defaults());
        Queue<SessionEvent> events = new ConcurrentLinkedQueue<>();
        runtime.addGlobalEventListener(events::add);

        // ① 业务会话产生两轮对话，其中一轮被用户打负反馈（boolean=false）
        try (var session = runtime.spawn("app", "agent", "demo-sess")) {
            model.enqueueText("正确答案 42");
            session.chat("生命的意义?");
            model.enqueueText("抱歉答错了");
            session.chat("光速是多少?");
            session.rateTurn(1, "boolean", "true", null, null);   // 正反馈（不入集）
            session.rateTurn(2, "boolean", "false", "答非所问", null); // 负反馈 → 回流原料
        }

        // ② 宿主建评估数据集，一键回流负反馈轮（带溯源）
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("demo-badcases", "演示坏例集");
        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);
        var imported = importer.importFromFeedback("demo-sess", "demo-badcases");
        assertThat(imported.imported()).isEqualTo(1);
        var item = datasetStore.items("demo-badcases").get(0);
        assertThat(item.input()).isEqualTo("光速是多少?");
        assertThat(item.expected()).isEqualTo("抱歉答错了");
        assertThat(item.sourceSessionId()).isEqualTo("demo-sess");

        // ③ 跑一轮评估（模型这次答对 → pass）
        model.enqueueText("抱歉答错了"); // 评估复跑同回复
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        var run = runner.run("demo-badcases", BuiltInEvaluators.EXACT);
        assertThat(run.passed()).isEqualTo(1);
        assertThat(run.passRate()).isEqualTo(1.0);

        // ④ 查询面：最新 run + 明细
        EvalQueryService query = new EvalQueryService(stores.sessionStateStore());
        var latest = query.latestRun("demo-badcases").orElseThrow();
        assertThat(latest.runId()).isEqualTo(run.runId());
        assertThat(latest.items().get(0).actualPreview()).isEqualTo("抱歉答错了");

        // ⑤ 事件到达（webhook 同通道）：eval.run.completed 含汇总
        SessionEvent completed = events.stream()
                .filter(e -> "eval.run.completed".equals(e.type()))
                .findFirst().orElseThrow();
        Map<String, Object> payload = completed.payload();
        assertThat(payload.get("datasetName")).isEqualTo("demo-badcases");
        assertThat(payload.get("passed")).isEqualTo(1);
        assertThat(payload.get("passRate")).isEqualTo(1.0);
    }

    /** 演示②：幂等回流 + 自定义评估器（宿主领域断言插入）。 */
    @Test
    void demoIdempotentImportAndCustomEvaluator() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        try (var session = runtime.spawn("app", "agent", "demo-2")) {
            model.enqueueText("包含答案的回复");
            session.chat("q");
            session.rateTurn(1, "numeric", "-1", null, null); // numeric 负值 = 负反馈
        }
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d2", null);
        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);
        assertThat(importer.importFromFeedback("demo-2", "d2").imported()).isEqualTo(1);
        assertThat(importer.importFromFeedback("demo-2", "d2").skippedDuplicate()).isEqualTo(1);

        // 宿主自定义评估器：只查「回复是否以指定前缀开头」（领域断言零改框架）
        model.enqueueText("包含答案的回复");
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        var run = runner.run("d2", (actual, expected, item) ->
                actual.startsWith("包含")
                        ? io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalScore.pass("前缀命中")
                        : io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalScore.fail("前缀未命中"));
        assertThat(run.passed()).isEqualTo(1);
    }
}
