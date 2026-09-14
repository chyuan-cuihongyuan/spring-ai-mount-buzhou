package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * eval 失败项重跑测试（spec 1635 / T2421–T2422 / impl 1188）：
 * run(..., onlyItemIds) 子集重跑——上轮 fail/error 的 id 传入即 rerun-failed
 * （total=子集数、新 runId）；null 全量零变化。
 */
class EvalRerunFailedTest {

    /** 定值评估器：期望为 "ok" 的项过、否则败。 */
    private static final Evaluator PASS_IF_OK = new Evaluator() {
        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            return "ok".equals(expected) ? EvalScore.pass("符合") : EvalScore.fail("不符");
        }
    };

    private static EvalDatasetStore seededStore(BuzhouStores stores) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ds", null);
        ds.addItem("ds", "q1", "ok", null, null);
        ds.addItem("ds", "q2", "bad", null, null);
        ds.addItem("ds", "q3", "ok", null, null);
        return ds;
    }

    private static EvalRunner runnerOf(BuzhouStores stores) {
        return new EvalRunner(
                Buzhou.runtime(new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel(),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                seededStore(stores), stores.sessionStateStore());
    }

    @Test
    void failedItemsRerunAsSubset() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runnerOf(stores);
        EvalRunResult full = runner.run("ds", PASS_IF_OK);
        assertThat(full.total()).isEqualTo(3);
        assertThat(full.failed()).isEqualTo(1);

        Set<String> failedIds = full.items().stream()
                .filter(i -> EvalRunItemResult.STATUS_FAIL.equals(i.status())
                        || EvalRunItemResult.STATUS_ERROR.equals(i.status()))
                .map(EvalRunItemResult::itemId)
                .collect(Collectors.toSet());
        assertThat(failedIds).hasSize(1); // q2 项

        EvalRunResult rerun = runner.run("ds", PASS_IF_OK, 1, failedIds);
        assertThat(rerun.total()).isEqualTo(1); // 子集
        assertThat(rerun.runId()).isNotEqualTo(full.runId()); // 新 run
        assertThat(rerun.items()).hasSize(1);
    }

    @Test
    void nullSubsetKeepsFullRun() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runnerOf(stores);
        EvalRunResult run = runner.run("ds", PASS_IF_OK, 1, null);
        assertThat(run.total()).isEqualTo(3); // 全量零变化
    }
}
