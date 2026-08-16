package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 52 §D / T193 / impl-159：批次评估 runner（顺序执行 + 逐项隔离 + 不断批 + run 记录）。
 */
class EvalRunnerTest {

    @Test
    void runsDatasetAndRecordsResultPerItem() {
        // 按调用计数脚本：call1→a1（pass）、call2→wrong（fail）、call3→抛（error）。
        // （ScriptedChatModel.throwOnCall 每次 call 优先消费，无法按时序排第三次——改计数替身）
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                seenPrompts.add(prompt);
                return switch (calls.incrementAndGet()) {
                    case 1 -> super.call(prompt);
                    case 2 -> super.call(prompt);
                    default -> throw new IllegalStateException("provider 崩了");
                };
            }
        };
        model.enqueueText("a1");
        model.enqueueText("wrong");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("suite", null);
        datasetStore.addItem("suite", "q1", "a1", null, null);
        datasetStore.addItem("suite", "q2", "a2", null, null);
        datasetStore.addItem("suite", "q3", "a3", null, null);

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        EvalRunResult result = runner.run("suite", BuiltInEvaluators.EXACT);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errored()).isEqualTo(1);
        assertThat(result.items()).extracting(EvalRunItemResult::status)
                .containsExactly("pass", "fail", "error");
        assertThat(result.items().get(2).detail()).contains("provider 崩了");
        assertThat(result.items().get(1).actualPreview()).isEqualTo("wrong");

        // run 记录落合成会话（eval.run. 前缀；往返同构）
        String key = EvalRunner.RUN_PREFIX + result.runId();
        String encoded = stores.sessionStateStore()
                .get(EvalDatasetStore.SESSION_ID, key).orElseThrow().value();
        EvalRunResult decoded = EvalRunner.mapToResult(EvalRunner.decodeMap(encoded));
        assertThat(decoded.runId()).isEqualTo(result.runId());
        assertThat(decoded.total()).isEqualTo(3);
        assertThat(decoded.passed()).isEqualTo(1);
        assertThat(decoded.passRate()).isEqualTo(1.0 / 3);

        // 评估会话隔离：项会话独立命名（不占业务命名空间），且已全部关闭
        assertThat(runtime.spawn("buzhou-eval", "eval", "probe-after-run")).isNotNull();
    }

    @Test
    void emptyDatasetYieldsZeroRunAndUnknownDatasetFailsFast() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("empty", null);

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        EvalRunResult result = runner.run("empty", BuiltInEvaluators.CONTAINS);
        assertThat(result.total()).isZero();
        assertThat(result.passRate()).isEqualTo(0.0);
        assertThat(stores.sessionStateStore()
                .scanByPrefix(EvalDatasetStore.SESSION_ID, EvalRunner.RUN_PREFIX)).hasSize(1);

        // 未建数据集：fail-fast 挂码
        assertThatThrownBy(() -> runner.run("missing", BuiltInEvaluators.EXACT))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode()
                        .name()).isEqualTo("EVAL_OPERATION_INVALID"));
    }

    /** 评估器返回 null：违反 SPI 契约按 error 记录（不静默不炸批）。 */
    @Test
    void nullScoreEvaluatorRecordedAsError() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("whatever");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d", null);
        datasetStore.addItem("d", "q", "e", null, null);

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        EvalRunResult result = runner.run("d", (actual, expected, item) -> null);
        assertThat(result.errored()).isEqualTo(1);
        assertThat(result.items().get(0).detail()).contains("返回 null");
    }
}
