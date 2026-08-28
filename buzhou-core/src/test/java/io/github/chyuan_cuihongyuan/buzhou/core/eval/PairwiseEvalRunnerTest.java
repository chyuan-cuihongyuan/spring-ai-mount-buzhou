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

/**
 * A/B 成对评估红队（spec 71 §B / T294）：A 路全胜（内容优势 + 内容型 judge）胜率 1.0；
 * 单路执行异常 → 该项 error 不裁胜负（分母不计）；项序确定；并行与串行等值同序。
 */
class PairwiseEvalRunnerTest {

    /** 回显模型：回复固定前缀 + 输入尾（内容可辨识——内容型 judge 按标记裁）。 */
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

    /** 内容型 judge：含 gold 标记的输出位赢（与位置无关——双向一致可裁）。 */
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

    private static PairwiseEvalRunner runner(BuzhouStores stores, ChatModel judge) {
        return new PairwiseEvalRunner(new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(judge));
    }

    private static void seedDataset(BuzhouStores stores, int items) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ab", null);
        for (int i = 1; i <= items; i++) {
            ds.addItem("ab", "q" + String.format("%02d", i), "ignored", null, null);
        }
    }

    @Test
    void contentSuperiorSideWinsAcrossDataset() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 4);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"),
                stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"),
                stores, RuntimeConfig.defaults());

        PairwiseEvalRunner.PairwiseEvalResult result = runner(stores, new GoldContentJudge())
                .compare("ab", runtimeA, runtimeB, 2);

        assertThat(result.summary().winsA()).isEqualTo(4);
        assertThat(result.summary().winRateA()).isEqualTo(1.0);
        assertThat(result.items()).allSatisfy(r -> {
            assertThat(r.error()).isNull();
            assertThat(r.outputA()).startsWith("gold-");
            assertThat(r.outputB()).startsWith("plain-");
        });
    }

    @Test
    void oneSideFailureIsErrorNotVerdict() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 2);
        ScriptedChatModel brokenB = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("B 路挂了");
            }
        };
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"),
                stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(brokenB, stores, RuntimeConfig.defaults());

        PairwiseEvalRunner.PairwiseEvalResult result = runner(stores, new GoldContentJudge())
                .compare("ab", runtimeA, runtimeB, 1);

        assertThat(result.summary().errors()).isEqualTo(2); // 两项皆 B 路异常
        assertThat(result.summary().winsA()).isZero();
        assertThat(result.items()).allSatisfy(r -> assertThat(r.error()).contains("B 路执行异常"));
    }

    @Test
    void parallelMatchesSerialOrderAndValues() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 5);
        PairwiseJudge judge = new PairwiseJudge(new GoldContentJudge());
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner r = new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()), judge);

        List<String> serial = r.compare("ab", runtimeA, runtimeB, 1).items().stream()
                .map(i -> i.itemId() + ":" + verdictOf(i)).toList();
        List<String> parallel = r.compare("ab", runtimeA, runtimeB, 4).items().stream()
                .map(i -> i.itemId() + ":" + verdictOf(i)).toList();
        assertThat(parallel).isEqualTo(serial); // 项序确定 + 裁决等值
    }

    private static String verdictOf(PairwiseEvalRunner.PairwiseItemResult r) {
        return r.error() != null ? "error" : r.verdict().winner().name();
    }
}
