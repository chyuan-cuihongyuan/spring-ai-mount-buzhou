package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 198 §B / T561：门禁宽松档红队——warnOnly=true 时脏数据集照跑（WARN
 * 带明细不拦）；单参 setExpectations 保持严格档（既有零变化）。
 */
class EvalRunnerWarnGateTest {

    @Test
    void warnOnlyRunsDirtyDatasetWhileStrictStillBlocks() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("dup", null);
        datasetStore.addItem("dup", "same", "a1", null, null);
        datasetStore.addItem("dup", "same", "a2", null, null); // 重复输入
        var runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());

        // 宽松档：照跑（items 全执行）
        runner.setExpectations(DatasetExpectations.of(DatasetExpectations.uniqueInputs()), true);
        assertThat(runner.run("dup", BuiltInEvaluators.EXACT).items()).hasSize(2);

        // 严格档（单参默认）：零变化——同数据集再严格装载后拦截
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            EvalRunner strict = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
            strict.setExpectations(DatasetExpectations.of(DatasetExpectations.uniqueInputs()));
            ScriptedChatModel unused = new ScriptedChatModel(); // 严格档模型零调用
            strict.run("dup", BuiltInEvaluators.EXACT);
        }).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException.class)
                .hasMessageContaining("unique-inputs");
    }
}
