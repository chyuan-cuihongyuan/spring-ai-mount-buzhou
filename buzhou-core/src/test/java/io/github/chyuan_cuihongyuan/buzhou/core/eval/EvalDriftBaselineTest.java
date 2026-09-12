package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 718 / T1036–T1037：评估通过率漂移基线——历史取样均值/告警触发/
 * 窗口限制/首跑跳过/默认关。
 */
class EvalDriftBaselineTest {

    private static ScriptedChatModel countingModel(AtomicInteger calls) {
        return new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                calls.incrementAndGet();
                return super.call(prompt);
            }
        };
    }

    /** 植入一次历史 run 记录（passRate 由 passCount/total 合成；startedAt 由秒序保证基线取样序）。 */
    private static void seedRun(BuzhouStores stores, String datasetName, Instant startedAt,
            int passCount, int total) {
        EvalRunResult run = new EvalRunResult("seed-" + startedAt.getEpochSecond(), datasetName,
                startedAt, startedAt.plusSeconds(1), total, passCount, total - passCount, 0,
                List.of(), null);
        stores.sessionStateStore().put(EvalDatasetStore.SESSION_ID,
                new StateEntry(EvalRunner.RUN_PREFIX + run.runId(), EvalRunner.encode(
                        EvalRunner.resultToMap(run)), "eval", 0, null, startedAt));
    }

    @Test
    void driftAgainstFullPassBaselineTriggers() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedRun(stores, "suite", Instant.parse("2026-09-12T00:00:01Z"), 3, 3);
        seedRun(stores, "suite", Instant.parse("2026-09-12T00:00:02Z"), 3, 3);
        seedRun(stores, "suite", Instant.parse("2026-09-12T00:00:03Z"), 3, 3);

        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        model.enqueueText("wrong"); // 模型输出偏离 → passRate 0
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("suite", null);
        datasetStore.addItem("suite", "a1", "a1", null, null);

        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setDriftBaseline(3, 0.2);
        assertThat(Double.isNaN(runner.lastDriftDelta())).isTrue(); // 未跑前 NaN

        EvalRunResult result = runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(result.passRate()).isZero();
        assertThat(runner.lastDriftDelta()).isEqualTo(-1.0); // 0 − 1.0

        // 历史样本不含本次：本 run 落盘后仍只有 3 条种子记录参与基线
    }

    @Test
    void windowLimitsSamplesAndFirstRunSkips() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedRun(stores, "s2", Instant.parse("2026-09-12T00:00:01Z"), 0, 3);
        seedRun(stores, "s2", Instant.parse("2026-09-12T00:00:02Z"), 0, 3);
        seedRun(stores, "s2", Instant.parse("2026-09-12T00:00:03Z"), 3, 3); // 最老窗口外应为 0 通过

        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        model.enqueueText("wrong");
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("s2", null);
        datasetStore.addItem("s2", "a1", "a1", null, null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setDriftBaseline(2, 0.5);
        EvalRunResult result = runner.run("s2", BuiltInEvaluators.EXACT);
        assertThat(result.passRate()).isZero();
        // window=2 → 基线 = 最近两次（0.0 + 1.0）/2 = 0.5 → delta = −0.5 ≥ 0.5 触发
        assertThat(runner.lastDriftDelta()).isEqualTo(-0.5);

        // 全新数据集首跑：无历史 → delta 保持上次值不更新、不告警（无异常即可）
        datasetStore.createDataset("fresh", null);
        datasetStore.addItem("fresh", "a1", "a1", null, null);
        model.enqueueText("a1");
        runner.run("fresh", BuiltInEvaluators.EXACT);
        assertThat(runner.lastDriftDelta()).isEqualTo(-0.5); // 无基线不改写
    }

    @Test
    void disabledByDefaultAndInvalidArgsFailFast() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("s3", null);
        datasetStore.addItem("s3", "a1", "a1", null, null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());

        // 默认关：漂移面零参与（lastDriftDelta 保持 NaN）
        runner.run("s3", BuiltInEvaluators.EXACT);
        assertThat(Double.isNaN(runner.lastDriftDelta())).isTrue();

        assertThatThrownBy(() -> runner.setDriftBaseline(-1, 0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> runner.setDriftBaseline(3, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> runner.setDriftBaseline(3, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
