package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 87 §B / T334：Ragas 系数值评估器红队——S x/y 协议解析（前缀容差/clamp）；
 * 阈值双向；分母 0 从严 0.0；协议失败/越界抛 JudgeProtocolException；两指标 prompt
 * 面正确（faithfulness 用 expected，relevancy 用 input）。借鉴：Ragas
 * faithfulness / answer-relevancy（连续分与二值 judge 互补）。
 */
class RagasEvaluatorsTest {

    private static ChatModel judgeOf(String reply) {
        return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
    }

    private static EvalItem item() {
        return new EvalItem("000001", "如何重置密码？", "进入设置-安全-重置密码", null, null,
                Instant.now());
    }

    @Test
    void faithfulnessParsesClaimRatioAndThresholdsBothWays() {
        EvalScore mid = RagasEvaluators.faithfulness(judgeOf("S 3/4 三条断言被支持"))
                .evaluate("进入设置可重置密码，还可以一键注销", "进入设置-安全-重置密码", item());

        assertThat(mid.passed()).isFalse(); // 0.75 < 0.8 默认阈值
        assertThat(mid.detail()).contains("score=0.750").contains("3/4").contains("threshold=0.8");

        EvalScore relaxed = RagasEvaluators.faithfulness(judgeOf("S 3/4"), 0.7)
                .evaluate("同上", "同上", item());
        assertThat(relaxed.passed()).isTrue(); // 0.75 >= 0.7

        // 前缀容差：空白 + 小写 s 也解析
        EvalScore tolerant = RagasEvaluators.faithfulness(judgeOf("  S 4/4 全支持"))
                .evaluate("a", "b", item());
        assertThat(tolerant.passed()).isTrue();
    }

    @Test
    void answerRelevancyParsesScoreOutOfTen() {
        EvalScore high = RagasEvaluators.answerRelevancy(judgeOf("S 9/10 直接回应"))
                .evaluate("设置-安全-重置密码", "ignored-expected", item());
        assertThat(high.passed()).isTrue();
        assertThat(high.detail()).contains("9/10");

        EvalScore low = RagasEvaluators.answerRelevancy(judgeOf("S 3/10 跑题"))
                .evaluate("密码是加密存储的", "ignored", item());
        assertThat(low.passed()).isFalse();
    }

    @Test
    void zeroDenominatorIsStrictZeroAndProtocolViolationsThrow() {
        EvalScore empty = RagasEvaluators.faithfulness(judgeOf("S 0/0 无断言"))
                .evaluate("(空)", "(空)", item());
        assertThat(empty.passed()).isFalse();
        assertThat(empty.detail()).contains("分母 0 按从严 0.0");

        assertThatThrownBy(() -> RagasEvaluators.faithfulness(judgeOf("我觉得不错"))
                .evaluate("a", "b", item()))
                .isInstanceOf(LlmJudgeEvaluator.JudgeProtocolException.class)
                .hasMessageContaining("S x/y");
        assertThatThrownBy(() -> RagasEvaluators.answerRelevancy(judgeOf("S 12/10 满分溢出"))
                .evaluate("a", "b", item()))
                .isInstanceOf(LlmJudgeEvaluator.JudgeProtocolException.class)
                .hasMessageContaining("越界");
    }

    @Test
    void promptsTargetTheRightReferenceText() {
        java.util.List<String> seen = new java.util.ArrayList<>();
        ChatModel capturing = prompt -> {
            seen.add(prompt.getInstructions().get(1).getText());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("S 1/1 ok"))));
        };
        String actual = "进入设置重置密码";
        String expected = "设置-安全-重置密码";

        RagasEvaluators.faithfulness(capturing).evaluate(actual, expected, item());
        RagasEvaluators.answerRelevancy(capturing).evaluate(actual, expected, item());

        String faithfulnessPrompt = seen.get(0);
        String relevancyPrompt = seen.get(1);
        // faithfulness 对照 expected（黄金答案）；relevancy 对照 input（用户问题）
        assertThat(faithfulnessPrompt).contains(expected).doesNotContain("【用户输入】");
        assertThat(relevancyPrompt).contains("如何重置密码？").contains("【用户输入】");
    }

    @Test
    void gEvalScoresCustomDimensionWithBothReferences() {
        java.util.List<String> seen = new java.util.ArrayList<>();
        ChatModel capturing = prompt -> {
            seen.add(prompt.getInstructions().get(1).getText());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("S 7/10 还行"))));
        };

        EvalScore result = RagasEvaluators.gEval(capturing, "合规性",
                "不得出现承诺收益的表述；风险提示完整", 0.6)
                .evaluate("收益可能翻倍", "稳健表述", item());

        assertThat(result.passed()).isTrue(); // 0.7 >= 0.6
        assertThat(result.detail()).startsWith("[合规性] score=0.700");
        // BOTH 参照系：输入与黄金答案都进 prompt
        String prompt = seen.getFirst();
        assertThat(prompt).contains("维度：合规性").contains("收益可能翻倍")
                .contains("稳健表述").contains("如何重置密码？");

        assertThatThrownBy(() -> RagasEvaluators.gEval(capturing, " ", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
