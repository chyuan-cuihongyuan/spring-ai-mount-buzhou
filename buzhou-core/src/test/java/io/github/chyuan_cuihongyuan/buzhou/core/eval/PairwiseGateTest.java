package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 101 §B / T376：A/B 胜率门红队——A 全胜过门 / B 占多不过门（预览含 WINNER_B）/
 * error 项进预览不进分母（spec 71 口径沿用）/ summary 单行。spec 80 fog 收口。
 */
class PairwiseGateTest {

    private static void seed(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 1; i <= n; i++) {
            ds.addItem(dataset, "q" + i, "ignored", null, null);
        }
    }

    @Test
    void aDominantPassesGateWithOkSummary() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "gate-a", 3);
        var runtimeA = Buzhou.runtime(new PairwiseEvalRunnerTest.EchoModel("gold-"),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runtimeB = Buzhou.runtime(new PairwiseEvalRunnerTest.EchoModel("plain-"),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        PairwiseGate gate = new PairwiseGate(new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(new PairwiseEvalRunnerTest.GoldContentJudge()),
                stores.sessionStateStore()));

        PairwiseGate.AbGateResult result = gate.enforce("gate-a", runtimeA, runtimeB, 1, 0.8);

        assertThat(result.passed()).isTrue(); // winRateA 1.0 >= 0.8
        assertThat(result.summary()).startsWith("ab-gate OK")
                .contains("winsA=3").contains("winRateA=1.000");
        assertThat(result.itemPreviews()).hasSize(3);
    }

    @Test
    void bDominantFailsWithWinnerBPreviews() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "gate-b", 3);
        // 交换：B 路带 gold 标记 → B 全胜
        var runtimeA = Buzhou.runtime(new PairwiseEvalRunnerTest.EchoModel("plain-"),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runtimeB = Buzhou.runtime(new PairwiseEvalRunnerTest.EchoModel("gold-"),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        PairwiseGate gate = new PairwiseGate(new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(new PairwiseEvalRunnerTest.GoldContentJudge()),
                stores.sessionStateStore()));

        PairwiseGate.AbGateResult result = gate.enforce("gate-b", runtimeA, runtimeB, 1, 0.8);

        assertThat(result.passed()).isFalse(); // winRateA 0.0 < 0.8
        assertThat(result.winRateB()).isEqualTo(1.0);
        assertThat(result.itemPreviews()).allMatch(p -> p.contains("[WINNER_B]"));
        // A 胜率下限门方向性：0.4 档对 A 全败仍不过（0.0 < 0.4）
        assertThat(gate.enforce("gate-b", runtimeA, runtimeB, 1, 0.4).passed()).isFalse();
    }

    @Test
    void errorsShownInPreviewsButExcludedFromRates() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "gate-err", 2);
        ScriptedChatModel flakyB = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("B 路全挂");
            }
        };
        var runtimeA = Buzhou.runtime(new PairwiseEvalRunnerTest.EchoModel("gold-"),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runtimeB = Buzhou.runtime(flakyB,
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        PairwiseGate gate = new PairwiseGate(new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(new PairwiseEvalRunnerTest.GoldContentJudge()),
                stores.sessionStateStore()));

        PairwiseGate.AbGateResult result = gate.enforce("gate-err", runtimeA, runtimeB, 1, 0.0);

        // error 全项：分母 0 → winRate 0.0；threshold clamp 0 → 0.0 >= 0.0 过（边界）
        assertThat(result.errors()).isEqualTo(2);
        assertThat(result.winRateA()).isZero();
        assertThat(result.passed()).isTrue();
        assertThat(result.itemPreviews()).allMatch(p -> p.contains("[error]"));
        assertThat(result.summary()).contains("errors=2/2");
    }
}
