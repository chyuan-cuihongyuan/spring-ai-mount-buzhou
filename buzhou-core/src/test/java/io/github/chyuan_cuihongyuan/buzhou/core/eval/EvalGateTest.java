package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
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
 * spec 80 §B / T314：评估回归门红队——阈值过/不过双向；error 计入分母（从严）；
 * 失败项预览（截断 + itemId/状态/detail）；CI 单行摘要可读；threshold clamp。
 * 借鉴：Promptfoo eval CI gate（--eval-min-pass-rate）/ LangSmith eval-as-gate。
 */
class EvalGateTest {

    /** 回显 runtime：输出 = 输入原文（让 EXACT 可控命中/不命中）。 */
    private static io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime echoRuntime(
            BuzhouStores stores) {
        ChatModel echo = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(
                prompt.getInstructions().getLast().getText()))));
        return Buzhou.runtime(echo, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
    }

    private static void seed(BuzhouStores stores, String dataset, int hits, int misses) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 1; i <= hits; i++) {
            ds.addItem(dataset, "hit" + i, "hit" + i, null, null);
        }
        for (int i = 1; i <= misses; i++) {
            ds.addItem(dataset, "miss" + i, "expected" + i, null, null);
        }
    }

    @Test
    void thresholdMetPassesWithOkSummary() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "ok-ds", 8, 2); // passRate 0.8
        EvalGate gate = new EvalGate(new EvalRunner(echoRuntime(stores),
                new EvalDatasetStore(stores.sessionStateStore()), stores.sessionStateStore()));

        EvalGate.GateResult result = gate.enforce("ok-ds", BuiltInEvaluators.EXACT, 0.8);

        assertThat(result.passed()).isTrue();
        assertThat(result.passRate()).isEqualTo(0.8);
        assertThat(result.summary()).startsWith("eval-gate OK")
                .contains("dataset=ok-ds").contains("passed=8").contains("failed=2");
        assertThat(result.failurePreviews()).hasSize(2); // 不过门也有预览（信息完整）
    }

    @Test
    void thresholdMissedFailsWithItemPreviews() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "bad-ds", 5, 5); // passRate 0.5
        EvalGate gate = new EvalGate(new EvalRunner(echoRuntime(stores),
                new EvalDatasetStore(stores.sessionStateStore()), stores.sessionStateStore()));

        EvalGate.GateResult result = gate.enforce("bad-ds", BuiltInEvaluators.EXACT, 0.9);

        assertThat(result.passed()).isFalse();
        assertThat(result.summary()).startsWith("eval-gate FAIL").contains("threshold=0.900");
        assertThat(result.failurePreviews()).hasSize(5);
        assertThat(result.failurePreviews().getFirst()).contains(" [fail] expected=");
        assertThat(result.failurePreviews()).anyMatch(p -> p.contains("actual=\"miss1\""));
    }

    @Test
    void errorsCountAgainstGateFromStrictSide() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("err-ds", null);
        ds.addItem("err-ds", "q1", "echo-ok", null, null);
        ds.addItem("err-ds", "boom", "never", null, null);
        // 模型对含 boom 的输入抛异常 → 该项 error（不计 passed）
        ScriptedChatModel flaky = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                String text = prompt.getInstructions().getLast().getText();
                if (text.contains("boom")) {
                    throw new IllegalStateException("模型炸了");
                }
                return new ChatResponse(List.of(new Generation(new AssistantMessage("echo-ok"))));
            }
        };
        EvalGate gate = new EvalGate(new EvalRunner(
                Buzhou.runtime(flaky, stores,
                        io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore()));

        EvalGate.GateResult result = gate.enforce("err-ds", BuiltInEvaluators.EXACT, 0.5);

        // passRate = 1/2 = 0.5：无 error 会过 0.5 门；error 在分母内恰好压线——边界用 0.51 证从严
        assertThat(result.errored()).isEqualTo(1);
        assertThat(result.passRate()).isEqualTo(0.5);
        EvalGate.GateResult strict = gate.enforce("err-ds", BuiltInEvaluators.EXACT, 0.51);
        assertThat(strict.passed()).isFalse(); // error 不是绿
        assertThat(strict.failurePreviews()).anyMatch(p -> p.contains("[error]"));
    }

    @Test
    void previewTruncatesBeyondLimitAndThresholdClamps() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seed(stores, "big-ds", 0, 15); // 15 失败
        EvalGate gate = new EvalGate(new EvalRunner(echoRuntime(stores),
                new EvalDatasetStore(stores.sessionStateStore()), stores.sessionStateStore()));

        EvalGate.GateResult truncated = gate.enforce("big-ds", BuiltInEvaluators.EXACT, 1.0);
        assertThat(truncated.failurePreviews()).hasSize(EvalGate.PREVIEW_LIMIT + 1);
        assertThat(truncated.failurePreviews().getLast()).contains("省略");

        EvalGate.GateResult clampedLow = gate.enforce("big-ds", BuiltInEvaluators.EXACT, -0.5);
        assertThat(clampedLow.threshold()).isZero(); // clamp 0：passRate 0.0 >= 0 过
        assertThat(clampedLow.passed()).isTrue();
        EvalGate.GateResult clampedHigh = gate.enforce("big-ds", BuiltInEvaluators.EXACT, 2.0);
        assertThat(clampedHigh.threshold()).isEqualTo(1.0);
        assertThat(clampedHigh.passed()).isFalse();
    }
}
