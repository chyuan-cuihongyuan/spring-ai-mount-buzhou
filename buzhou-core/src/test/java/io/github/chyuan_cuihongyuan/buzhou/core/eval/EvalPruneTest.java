package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-654 / spec 901：评估失败率中途剪枝——恰停（观察窗外失败率达阈值→剩余项
 * pruned 未执行）、观察窗内不放行、默认关闭零行为回归、并行路径诚实不生效、
 * 阈值未达不剪、策略参数校验。
 */
class EvalPruneTest {

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

    /** 恒定语义 fail 的评估器（计数调用验证剪枝止损）。 */
    private static final class AlwaysFailEvaluator implements Evaluator {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            calls.incrementAndGet();
            return EvalScore.fail("语义不符");
        }

        int calls() {
            return calls.get();
        }
    }

    /** 恒定 pass 的评估器（阈值未达不剪）。 */
    private static final class AlwaysPassEvaluator implements Evaluator {
        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            return EvalScore.pass("ok");
        }
    }

    @Test
    void prunesRemainingItemsOnceFailRateReached() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-prune", 6);
        runner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-prune", evaluator);

        // 观察窗 2 项跑完即达阈值（2/2 ≥ 0.5）→ 恰停：2 项真实执行 + 4 项 pruned
        assertThat(evaluator.calls()).isEqualTo(2);
        assertThat(run.total()).isEqualTo(6);
        assertThat(run.items()).hasSize(6);
        assertThat(run.items().subList(0, 2))
                .allSatisfy(r -> assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_FAIL));
        assertThat(run.items().subList(2, 6))
                .allSatisfy(r -> {
                    assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_PRUNED);
                    assertThat(r.detail()).contains("[PRUNED]");
                });
        assertThat(run.failed()).isEqualTo(2);
        assertThat(run.errored()).isZero();
    }

    @Test
    void observationWindowPreventsPrematurePrune() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-window", 3);
        // 观察窗=3（全部项数）：即使全 fail 也不会触发（判定窗口 = 跑满 minItems 后）——
        // 全 fail 时最后一项完成后 bad/3=1.0 ≥ 0.5 触发，但已无剩余项可剪
        runner.setPrunePolicy(new EvalPrunePolicy(3, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-window", evaluator);

        assertThat(evaluator.calls()).isEqualTo(3);
        assertThat(run.items()).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_FAIL));
    }

    @Test
    void disabledByDefaultRunsEverything() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-default", 4);
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-default", evaluator);

        // 默认无剪枝：零行为回归——4 项全跑全 fail
        assertThat(evaluator.calls()).isEqualTo(4);
        assertThat(run.items()).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_FAIL));
    }

    @Test
    void parallelPathHonestNoPrune() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-parallel", 5);
        runner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();

        EvalRunResult run = runner.run("ds-parallel", evaluator, 4);

        // 并行路径诚实不剪（spec 901 入档边界）：5 项全跑
        assertThat(evaluator.calls()).isEqualTo(5);
        assertThat(run.items()).allSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_FAIL));
    }

    @Test
    void noPruneWhenThresholdNotReached() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-pass", 4);
        runner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));

        EvalRunResult run = runner.run("ds-pass", new AlwaysPassEvaluator());

        assertThat(run.passed()).isEqualTo(4);
        assertThat(run.items()).noneSatisfy(r ->
                assertThat(r.status()).isEqualTo(EvalRunItemResult.STATUS_PRUNED));
    }

    @Test
    void policyArgsValidated() {
        assertThatThrownBy(() -> new EvalPrunePolicy(0, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalPrunePolicy(2, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalPrunePolicy(2, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
