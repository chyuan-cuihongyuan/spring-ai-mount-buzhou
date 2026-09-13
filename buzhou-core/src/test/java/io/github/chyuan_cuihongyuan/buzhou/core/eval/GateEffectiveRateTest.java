package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-686 / spec 934：GateResult 有效通过率透出——剪枝 run 填充精确（总量口径
 * 稀释显形 + 有效口径排除 pruned）、无剪枝双口径相等、10 参兼容构造 NaN 委托、
 * passed 判定语义零变化。
 */
class GateEffectiveRateTest {

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

    @Test
    void prunedRunCarriesEffectiveRateThroughGate() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-gate-eff", 5);
        runner.setPrunePolicy(new EvalPrunePolicy(3, 0.5));
        EvalGate gate = new EvalGate(runner);

        // 条件 evaluator：前 2 项 pass、之后 fail——窗 3 完成时 1/3 < 0.5 不剪；
        // 窗 4 完成时 2/4 = 0.5 ≥ 0.5 剪 → 第 5 项 pruned
        AtomicInteger seq = new AtomicInteger();
        EvalGate.GateResult result = gate.enforce("ds-gate-eff", (actual, expected, item) -> {
            int n = seq.getAndIncrement();
            return n < 2 ? EvalScore.pass("ok") : EvalScore.fail("x");
        }, 0.5);

        // 总量口径稀释显形：2 pass / 5 total = 0.4；有效口径 2/4 = 0.5
        assertThat(result.passRate()).isCloseTo(0.4, within(1e-9));
        assertThat(result.effectivePassRate()).isCloseTo(0.5, within(1e-9));
        // passed 判定语义零变化：总量口径 0.4 < 0.5 → FAIL
        assertThat(result.passed()).isFalse();
    }

    @Test
    void noPruneBothRatesEqual() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-gate-noprune", 3);
        EvalGate gate = new EvalGate(runner);
        EvalGate.GateResult result = gate.enforce("ds-gate-noprune",
                (actual, expected, item) -> EvalScore.pass("ok"), 0.5);
        assertThat(result.effectivePassRate()).isEqualTo(result.passRate());
    }

    @Test
    void legacyConstructorDelegatesNaN() {
        EvalGate.GateResult legacy = new EvalGate.GateResult(true, "ds", "r",
                1.0, 0.5, 2, 2, 0, 0, List.of());
        assertThat(legacy.effectivePassRate()).isNaN(); // 旧形态语义「未计算」
    }
}
