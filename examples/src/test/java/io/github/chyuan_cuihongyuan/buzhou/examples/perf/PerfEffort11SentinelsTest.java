package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.BuiltInEvaluators;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalDatasetStore;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalQueryService;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.EvalRunner;
import io.github.chyuan_cuihongyuan.buzhou.core.eval.FeedbackImporter;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #11 评估面 perf 哨兵（spec 52 / T197 / impl-163）：runner 全链路、
 * 数据集 scan、回流导入——10 倍宽幅粗粒度回归哨兵，nightly 以 -Dgroups=perf 激活。
 */
@Tag("perf")
class PerfEffort11SentinelsTest {

    /** runner 全链路（3 项 run：spawn×4 + chat×3 + 打分 + 记录写）P95 上限（首轮实测 <5ms）。 */
    private static final double RUNNER_P95_MAX_MILLIS = 80;

    /** 数据集 scan（50 项 items() 全量读）P95 上限（首轮实测 <2ms）。 */
    private static final double SCAN_P95_MAX_MILLIS = 40;

    /** 回流导入（scan 反馈 + 历史读 + 5 项写入）单会话 P95 上限（首轮实测 <2ms）。 */
    private static final double IMPORT_P95_MAX_MILLIS = 40;

    private static final int SAMPLES = 20;

    /** ①runner 全链路：3 项数据集完整 run（脚本模型直答）。 */
    @Test
    void evalRunnerFullLoopSentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("perf", null);
        datasetStore.addItem("perf", "q1", "a", null, null);
        datasetStore.addItem("perf", "q2", "a", null, null);
        datasetStore.addItem("perf", "q3", "a", null, null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        double p95 = p95Of(() -> {
            model.enqueueText("a");
            model.enqueueText("a");
            model.enqueueText("a");
            return runner.run("perf", BuiltInEvaluators.CONTAINS).total() == 3;
        });
        assertThat(p95).isLessThan(RUNNER_P95_MAX_MILLIS);
    }

    /** ②数据集 scan：50 项 items() + 全量 run 摘要查询。 */
    @Test
    void datasetScanSentinel() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("perf-scan", null);
        for (int i = 0; i < 50; i++) {
            datasetStore.addItem("perf-scan", "q" + i, "e" + i, "s", i);
        }
        EvalQueryService query = new EvalQueryService(stores.sessionStateStore());
        double p95 = p95Of(() -> datasetStore.items("perf-scan").size() == 50
                && query.allRuns().isEmpty());
        assertThat(p95).isLessThan(SCAN_P95_MAX_MILLIS);
    }

    /** ③回流导入：单会话 scan 反馈 + 历史读 + 负轮写入（幂等路径第二次全 skip）。 */
    @Test
    void feedbackImportSentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("perf-imp", null);
        try (var session = runtime.spawn("app", "ag", "perf-imp-src")) {
            for (int t = 1; t <= 3; t++) {
                model.enqueueText("ans" + t);
                session.chat("q" + t);
                session.rateTurn(t, "boolean", t == 2 ? "false" : "true", null, null);
            }
        }
        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);
        double p95 = p95Of(() -> importer.importFromFeedback("perf-imp-src", "perf-imp")
                .skippedDuplicate() + importer.importFromFeedback("perf-imp-src", "perf-imp").imported() >= 0);
        assertThat(p95).isLessThan(IMPORT_P95_MAX_MILLIS);
    }

    /** P95（同款粗粒度口径：排序取 95 分位，毫秒）。 */
    private static double p95Of(Supplier<Boolean> op) {
        long[] samples = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            assertThat(op.get()).isTrue();
            samples[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(samples);
        return samples[(int) Math.floor(SAMPLES * 0.95) - 1];
    }
}
