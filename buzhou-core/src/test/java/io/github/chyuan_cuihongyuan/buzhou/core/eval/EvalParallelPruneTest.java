package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1522 / T2295–T2296：并行路径失败率剪枝做实（spec 901「并行诚实不生效」
 * 边界收口）——分波执行 + 波间观察窗检查：恒 fail 评估器首波达阈值后剩余 pruned；
 * 未配策略零行为（全量执行）。
 */
class EvalParallelPruneTest {

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

    /** 恒 fail 评估器（计数调用——剪枝止损验证）。 */
    private static final class AlwaysFailEvaluator implements Evaluator {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            calls.incrementAndGet();
            return EvalScore.fail("语义不符");
        }
    }

    /** ① 并行 + 剪枝：首波（workers=2）双 fail 达阈值（minItems=2, 阈值 0.5）→ 剩余 pruned。 */
    @Test
    void parallelPruneShouldStopAfterThresholdWave() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-pp", 8);
        evalRunner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult result = evalRunner.run("ds-pp", evaluator, 2);

        // 首波 2 项执行（失败率 1.0 ≥ 0.5）→ 后续 3 波不再起：评估器恰调 2 次
        assertThat(evaluator.calls.get()).isEqualTo(2);
        assertThat(result.items()).hasSize(8);
        long pruned = result.items().stream()
                .filter(r -> EvalRunItemResult.STATUS_PRUNED.equals(r.status())).count();
        assertThat(pruned).isEqualTo(6);
        assertThat(result.failed()).isEqualTo(2);
    }

    /** ② 未配策略的并行 run 零变化：全量执行无 pruned。 */
    @Test
    void parallelWithoutPolicyShouldRunAll() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-pp-full", 6);

        EvalRunResult result = evalRunner.run("ds-pp-full", new AlwaysFailEvaluator(), 2);

        assertThat(result.items()).hasSize(6);
        assertThat(result.items())
                .noneMatch(r -> EvalRunItemResult.STATUS_PRUNED.equals(r.status()));
        assertThat(result.failed()).isEqualTo(6);
    }

    /** ③ 阈值未达不剪：fail/pass 混合低于阈值 → 全量执行。 */
    @Test
    void parallelBelowThresholdShouldNotPrune() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-pp-below", 6);
        evalRunner.setPrunePolicy(new EvalPrunePolicy(2, 0.9));
        // pass 评估器：失败率 0 恒不达阈——不剪
        EvalRunResult result = evalRunner.run("ds-pp-below", new Evaluator() {
            @Override
            public EvalScore evaluate(String actual, String expected, EvalItem item) {
                return EvalScore.pass("ok");
            }
        }, 2);

        assertThat(result.items()).hasSize(6);
        assertThat(result.passed()).isEqualTo(6);
        assertThat(result.items())
                .noneMatch(r -> EvalRunItemResult.STATUS_PRUNED.equals(r.status()));
    }
}
