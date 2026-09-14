package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPRT 序贯提前终止测试（spec 1605 / T2361–T2362 / impl 1158）：判定器边界
 * （连续胜局达界/对称/初期继续/参数校验）+ runner 集成（A 恒胜大集提前停 +
 * skipped 桶 + 决策入 summary；未启用零变化）。Wald SPRT / sequential testing 思想。
 */
class PairwiseSprtPolicyTest {

    @Test
    void consecutiveWinsReachDecisionAndEarlyStaysContinue() {
        PairwiseSprtPolicy policy = PairwiseSprtPolicy.defaults();
        // α=0.05/β=0.10：上界 ln18≈2.89；连胜 4 项 LLR=4ln2≈2.77 未达界，5 项 3.47 达界
        assertThat(policy.decide(4, 0)).isEqualTo(PairwiseSprtPolicy.Decision.CONTINUE);
        assertThat(policy.decide(5, 0)).isEqualTo(PairwiseSprtPolicy.Decision.PREFER_A);
        assertThat(policy.decide(0, 5)).isEqualTo(PairwiseSprtPolicy.Decision.PREFER_B);
        assertThat(policy.decide(0, 0)).isEqualTo(PairwiseSprtPolicy.Decision.CONTINUE);
        assertThat(policy.decide(3, 3)).isEqualTo(PairwiseSprtPolicy.Decision.CONTINUE);
    }

    @Test
    void invalidAlphaBetaFailsFast() {
        assertThatThrownBy(() -> new PairwiseSprtPolicy(0, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alpha");
        assertThatThrownBy(() -> new PairwiseSprtPolicy(0.05, 0.6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beta");
    }

    // —— runner 集成（复用 PairwiseEvalRunnerTest 的伪件模式） ——

    static final class EchoModel extends ScriptedChatModel {
        private final String prefix;

        EchoModel(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = prompt.getInstructions().getLast().getText();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    prefix + text.substring(text.length() - 2)))));
        }
    }

    static final class GoldContentJudge implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            String user = prompt.getInstructions().get(1).getText();
            int a = user.indexOf("【输出A】") + "【输出A】".length();
            int b = user.indexOf("【输出B】", a);
            String slotA = user.substring(a, b);
            String winner = slotA.contains("gold") ? "WINNER_A" : "WINNER_B";
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    winner + " 内容更完整"))));
        }
    }

    private static void seedDataset(BuzhouStores stores, int items) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ab", null);
        for (int i = 1; i <= items; i++) {
            ds.addItem("ab", "q" + String.format("%02d", i), "ignored", null, null);
        }
    }

    @Test
    void earlyStopSkipsRemainderWhenSignificanceReached() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 40); // 全量 40 项远超达界所需（串行第 5 项达界）
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner runner = new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()), new PairwiseJudge(new GoldContentJudge()));

        PairwiseEvalRunner.PairwiseEvalResult result = runner.compare("ab", runtimeA, runtimeB, 1,
                PairwiseSprtPolicy.defaults());

        assertThat(result.summary().sprtDecision()).isEqualTo("PREFER_A");
        assertThat(result.summary().winsA()).isEqualTo(5);
        assertThat(result.summary().skipped()).isEqualTo(35); // 串行序贯：5 项达界即停
        assertThat(result.summary().total()).isEqualTo(40);
    }

    @Test
    void disabledPolicyRunsFullDatasetZeroChange() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 8);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner runner = new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()), new PairwiseJudge(new GoldContentJudge()));

        PairwiseEvalRunner.PairwiseEvalResult result = runner.compare("ab", runtimeA, runtimeB, 1);

        assertThat(result.summary().sprtDecision()).isNull();
        assertThat(result.summary().skipped()).isZero();
        assertThat(result.summary().winsA()).isEqualTo(8);
    }
}
