package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-694 续 / spec 953：pass@k×防抖门组合语义——同数据「频率门不达标 /
 * 概率口径达标」双结论并存不矛盾（互补口径）；enforceStable×history 一致性。
 */
class PassAtKGateComboTest {

    @Test
    void dualVerdictsCoexist() {
        double[] singleRunPassRates = {0.0, 1.0}; // 两次 run 各 1 项：一次 fail 一次 pass
        // 频率门（单次 0.5 视角）：对「1 pass / 2 total」的单 run，0.5 < 0.6 不达标
        // 概率口径（spec 902）：n=2 采样 c=1 → pass@2 = 1（必然有一次通过）≥ 0.6 达标
        double passAt2 = EvalPassAtK.estimate(2, 1, 2);
        assertThat(passAt2).isCloseTo(1.0, within(1e-12));
        assertThat(singleRunPassRates[0]).isLessThan(0.6);
        // 双结论并存：概率口径达标不蕴含单次频率门通过——互不替代
    }

    @Test
    void stableGateHistoryConsistentWithK() {
        io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores =
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ds-combo", null);
        ds.addItem("ds-combo", "q", "ok", null, null);
        EvalGate gate = new EvalGate(new EvalRunner(
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.runtime(
                        prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore()));

        EvalGate.GateResult last = gate.enforceStable("ds-combo",
                (a, b, c) -> EvalScore.pass("ok"), 0.5, 3);

        assertThat(last.passed()).isTrue();
        assertThat(gate.history()).hasSize(3); // k 次判定全留史（与 spec 914 环形容量一致性）
        assertThat(EvalGate.thresholdDrift(gate.history()).transitions()).isZero();
    }
}
