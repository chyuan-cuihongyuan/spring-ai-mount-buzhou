package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LLM-as-judge 评估器红队（spec 61 §B / T274）：协议首词解析（PASS/FAIL/大小写/前缀
 * 文案容差）、不可解析抛协议异常、rubric 注入进 prompt、judge 异常传播、单条 detail
 * 截断；runner 收敛异常为该条 error 不炸整跑。
 */
class LlmJudgeEvaluatorTest {

    /** 固定响应的 stub judge（捕获 prompt 供断言）。 */
    static final class StubJudge implements ChatModel {
        volatile String response;
        volatile Prompt lastPrompt;

        StubJudge(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPrompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }
    }

    private static final EvalItem ITEM = new EvalItem("i1", "问题", "期望",
            "src-sess", 3, java.time.Instant.parse("2026-08-29T00:00:00Z"));

    @Test
    void passFailVerdictsParseWithTolerance() {
        assertThat(new LlmJudgeEvaluator(new StubJudge("PASS 语义一致")).evaluate(
                "实际", "期望", ITEM).passed()).isTrue();
        assertThat(new LlmJudgeEvaluator(new StubJudge("FAIL 缺关键事实")).evaluate(
                "实际", "期望", ITEM).passed()).isFalse();
        // 大小写 + 首尾空白 + 前缀文案容差
        assertThat(new LlmJudgeEvaluator(new StubJudge("  pass 回答达标")).evaluate(
                "实际", "期望", ITEM).passed()).isTrue();
        assertThat(new LlmJudgeEvaluator(new StubJudge("\nFAIL\n多行理由")).evaluate(
                "实际", "期望", ITEM).passed()).isFalse();
    }

    @Test
    void verdictDetailCarriesJudgeReason() {
        EvalScore score = new LlmJudgeEvaluator(new StubJudge("FAIL 缺少订单号")).evaluate(
                "实际", "期望", ITEM);
        assertThat(score.detail()).contains("judge:").contains("缺少订单号");
    }

    @Test
    void unparseableResponseThrowsProtocolException() {
        StubJudge judge = new StubJudge("我觉得差不多，可以过吧");
        assertThatThrownBy(() -> new LlmJudgeEvaluator(judge).evaluate("实际", "期望", ITEM))
                .isInstanceOf(LlmJudgeEvaluator.JudgeProtocolException.class)
                .hasMessageContaining("PASS/FAIL");
    }

    @Test
    void customRubicReachesJudgePrompt() {
        StubJudge judge = new StubJudge("PASS");
        new LlmJudgeEvaluator(judge, "回复必须包含法律条款编号").evaluate("实际", "期望", ITEM);
        String system = judge.lastPrompt.getInstructions().getFirst().getText();
        assertThat(system).contains("法律条款编号"); // rubric 注入
        assertThat(system).contains("PASS 或 FAIL"); // 协议固定
        String user = judge.lastPrompt.getInstructions().get(1).getText();
        assertThat(user).contains("【期望输出】").contains("期望")
                .contains("【实际输出】").contains("实际"); // 分段不拼 JSON
    }

    @Test
    void judgeFailurePropagatesForRunnerToRecord() {
        ChatModel broken = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("api down");
            }
        };
        assertThatThrownBy(() -> new LlmJudgeEvaluator(broken).evaluate("实际", "期望", ITEM))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api down");
    }
}
