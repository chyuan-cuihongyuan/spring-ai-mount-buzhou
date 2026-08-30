package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 150 §B / T503：期望门禁接线红队——脏数据集 run 前 fail-fast（模型零
 * 调用——脏数据零 token 成本出局，message 带 summary + 前三条发现）；干净
 * 数据集照常跑；未装载门禁零变化。
 */
class EvalRunnerGateTest {

    @Test
    void dirtyDatasetFailsFastBeforeAnyModelCall() {
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                calls.incrementAndGet();
                return super.call(prompt);
            }
        };
        model.enqueueText("never-used");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("dirty", null);
        // store 层已拒空 input/expected——脏面走「重复行 + 回流项缺溯源」
        datasetStore.addItem("dirty", "同问", "a1", null, null);
        datasetStore.addItem("dirty", "同问", "a2", null, null); // 重复输入

        var runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan
                .buzhou.core.session.RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setExpectations(DatasetExpectations.of(
                DatasetExpectations.uniqueInputs(),
                DatasetExpectations.named("reflow-requires-source",
                        item -> item.sourceSessionId() != null)));

        assertThatThrownBy(() -> runner.run("dirty", BuiltInEvaluators.EXACT))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.EVAL_OPERATION_INVALID))
                .hasMessageContaining("数据集期望门禁未过")
                .hasMessageContaining("unique-inputs")
                .hasMessageContaining("reflow-requires-source");
        assertThat(calls.get()).isZero(); // 模型零调用：脏数据零 token 成本
    }

    @Test
    void cleanDatasetPassesGateAndRunsNormally() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("clean", null);
        datasetStore.addItem("clean", "q1", "a1", null, null);

        var runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan
                .buzhou.core.session.RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setExpectations(DatasetExpectations.of(
                DatasetExpectations.nonBlankInputs(),
                DatasetExpectations.expectedPresent(),
                DatasetExpectations.sizeBetween(1, 100)));

        EvalRunResult result = runner.run("clean", BuiltInEvaluators.EXACT);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).status())
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalRunItemResult.STATUS_PASS);
    }

    @Test
    void noGateConfiguredKeepsLegacyBehavior() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("legacy", null);
        datasetStore.addItem("legacy", "dup", "a1", null, null);
        datasetStore.addItem("legacy", "dup", "a2", null, null); // 无门禁：重复行照跑
        model.enqueueText("a2");

        var runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan
                .buzhou.core.session.RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        assertThat(runner.run("legacy", BuiltInEvaluators.EXACT).items()).hasSize(2);
    }
}
