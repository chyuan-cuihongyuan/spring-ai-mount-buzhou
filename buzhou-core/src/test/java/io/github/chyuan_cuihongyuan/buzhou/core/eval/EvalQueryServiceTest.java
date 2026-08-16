package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 52 §E / T194 / impl-160：评估结果只读查询面（列表过滤/倒序/明细/最新）。
 */
class EvalQueryServiceTest {

    @Test
    void listsFiltersAndReturnsLatestAndDetail() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        datasetStore.createDataset("alpha", null);
        datasetStore.createDataset("beta", null);
        datasetStore.addItem("alpha", "q", "a", null, null);
        datasetStore.addItem("beta", "q", "b", null, null);

        model.enqueueText("a");
        EvalRunResult alpha1 = runner.run("alpha", BuiltInEvaluators.EXACT);
        Thread.sleep(3); // startedAt 毫秒可分（倒序断言稳定性）
        model.enqueueText("wrong");
        EvalRunResult alpha2 = runner.run("alpha", BuiltInEvaluators.EXACT); // 第二次 run（fail）
        model.enqueueText("b");
        runner.run("beta", BuiltInEvaluators.EXACT);

        EvalQueryService query = new EvalQueryService(stores.sessionStateStore());
        // 全量 3 run 倒序（startedAt）
        assertThat(query.allRuns()).hasSize(3);
        // dataset 过滤：alpha 2 run、beta 1 run
        assertThat(query.runs("alpha")).hasSize(2);
        assertThat(query.runs("beta")).hasSize(1);
        // 最新 = 后跑的 fail run
        var latest = query.latestRun("alpha").orElseThrow();
        assertThat(latest.runId()).isEqualTo(alpha2.runId()).isNotEqualTo(alpha1.runId());
        assertThat(latest.passed()).isZero();
        // 明细含逐项 actual
        assertThat(latest.items()).hasSize(1);
        assertThat(latest.items().get(0).actualPreview()).isEqualTo("wrong");
        // 未知 runId
        assertThat(query.run("r-nonexistent")).isEmpty();
        assertThat(query.latestRun("missing")).isEmpty();
    }
}
