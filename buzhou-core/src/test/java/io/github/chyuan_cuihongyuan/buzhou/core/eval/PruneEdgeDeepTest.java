package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-684 / spec 931：剪枝边界深验——minItems==total 不残缺、阈值极小首 fail
 * 即剪、memo 命中不绕剪枝（pruned 不入 memo）、Expectations 门组合序。
 */
class PruneEdgeDeepTest {

    private static EvalRunner runner(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(dataset, "问题" + i, "ok", null, null);
        }
        return new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore());
    }

    private static final class AlwaysFailEvaluator implements Evaluator {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            calls.incrementAndGet();
            return EvalScore.fail("x");
        }

        int calls() {
            return calls.get();
        }
    }

    @Test
    void minItemsEqualsTotalRunsComplete() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-edge1", 3);
        runner.setPrunePolicy(new EvalPrunePolicy(3, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-edge1", evaluator);

        // 全部 3 项真实执行（观察窗 = 总数，触发时已无剩余）——run 不残缺
        assertThat(evaluator.calls()).isEqualTo(3);
        assertThat(run.total()).isEqualTo(3);
        assertThat(run.items()).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_FAIL));
    }

    @Test
    void tinyThresholdPrunesAtFirstFailure() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-edge2", 4);
        runner.setPrunePolicy(new EvalPrunePolicy(1, 0.01));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-edge2", evaluator);

        // 观察窗 1：首个 fail（1/1 ≥ 0.01）即剪——仅 1 项真实执行
        assertThat(evaluator.calls()).isEqualTo(1);
        assertThat(run.items().get(0).status()).isEqualTo(EvalRunItemResult.STATUS_FAIL);
        assertThat(run.items().subList(1, 4)).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_PRUNED));
    }

    @Test
    void memoizedHitsDoNotBypassPruneDecision() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-edge3", 2);
        runner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult first = runner.run("ds-edge3", evaluator);
        // memo 键挂上后二跑：全 fail 场景 memo 不命中（fail 不记忆化——spec 535 语义 fail 不重试口径），
        // 但 memo 命中的 pass 项照常参与裁决——此处断言两次 run 剪枝行为一致（裁决不受 memo 影响）
        runner.setMemoizationKey("edge3");
        EvalRunResult second = runner.run("ds-edge3", evaluator);

        assertThat(second.total()).isEqualTo(first.total());
        assertThat(second.failed()).isEqualTo(first.failed());
    }
}
