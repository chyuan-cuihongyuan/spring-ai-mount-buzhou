package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 525 / T803–T804：评估集合成扩增——正常扩增候选、围栏+坏行容错、
 * 零可解析异常带预览、count 指令透传、null fail-fast。人审教义：只产
 * 候选不入库（Ragas testset generation）。
 */
class EvalCaseAmplifierTest {

    private static final EvalItem SEED = new EvalItem(
            "c-000001", "查订单 ORD-1 状态", "已发货", null, null, null);

    private static ChatModel canned(String text) {
        return prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage(text))));
    }

    @Test
    void amplifiesParaphrasedCandidates() {
        String modelOutput = """
                {"input":"ORD-1 的物流状态","expected":"已发货"}
                {"input":"帮我看看订单 ORD-1 到哪了","expected":"已发货"}
                {"input":"ORD-1 发货没有","expected":"已发货"}
                """;
        EvalCaseAmplifier amplifier = new EvalCaseAmplifier(canned(modelOutput));
        List<EvalItem> candidates = amplifier.amplify(SEED, 3);
        assertThat(candidates).hasSize(3);
        assertThat(candidates.get(0).input()).isEqualTo("ORD-1 的物流状态");
        assertThat(candidates.get(0).expected()).isEqualTo("已发货");
        assertThat(candidates.get(0).id()).isNull(); // 候选未入库——id 留空
        assertThat(candidates.get(0).sourceSessionId()).isNull(); // 合成项非回流项
    }

    @Test
    void fencesAndBadLinesAreSkipped() {
        String modelOutput = """
                ```json
                这行不是 JSON
                {"input":"变体一","expected":"ok"}
                {"坏行":true}
                {"input":"变体二","expected":"ok"}
                ```""";
        EvalCaseAmplifier amplifier = new EvalCaseAmplifier(canned(modelOutput));
        List<EvalItem> candidates = amplifier.amplify(SEED, 5);
        assertThat(candidates).extracting(EvalItem::input)
                .containsExactly("变体一", "变体二");
    }

    @Test
    void zeroParsableFailsWithPreview() {
        EvalCaseAmplifier amplifier = new EvalCaseAmplifier(canned("抱歉我不会"));
        assertThatThrownBy(() -> amplifier.amplify(SEED, 2))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("无可解析用例")
                .hasMessageContaining("抱歉我不会");
    }

    @Test
    void countInstructionCarriedToModel() {
        final String[] seen = new String[1];
        ChatModel capturing = prompt -> {
            seen[0] = prompt.getInstructions().get(0).getText();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "{\"input\":\"x\",\"expected\":\"y\"}"))));
        };
        new EvalCaseAmplifier(capturing).amplify(SEED, 4);
        assertThat(seen[0]).contains("4 条").contains("查订单 ORD-1 状态");
    }

    @Test
    void nullSeedAndInvalidCountFailFast() {
        EvalCaseAmplifier amplifier = new EvalCaseAmplifier(canned("{}"));
        assertThatThrownBy(() -> amplifier.amplify(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> amplifier.amplify(SEED, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalCaseAmplifier(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
