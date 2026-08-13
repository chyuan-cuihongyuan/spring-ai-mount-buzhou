package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-01 测试基建自举测试：脚本消费/耗尽重复末值/并行块语义/录制回放往返/失配即失败。
 */
class FakeChatModelTest {

    @Test
    void scriptConsumedInOrderAndRepeatsLastAfterExhaustion() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.text("first"), ScriptStep.text("second"));

        assertThat(textOf(model.call(new Prompt(List.of(new UserMessage("q1")))))).isEqualTo("first");
        assertThat(textOf(model.call(new Prompt(List.of(new UserMessage("q2")))))).isEqualTo("second");
        // 耗尽后重复末步（mockValues 语义）
        assertThat(textOf(model.call(new Prompt(List.of(new UserMessage("q3")))))).isEqualTo("second");
        assertThat(model.callCount()).isEqualTo(3);
        assertThat(model.seenPrompts).hasSize(3);
    }

    @Test
    void parallelStepEmitsSingleAssistantMessageWithMultipleToolCalls() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.parallel(new ToolCallSpec("tool_a", "{}"), new ToolCallSpec("tool_b", "{}")),
                ScriptStep.text("done"));

        ChatResponse response = model.call(new Prompt(List.of(new UserMessage("并行查"))));
        AssistantMessage assistant = response.getResult().getOutput();
        assertThat(assistant.getToolCalls()).hasSize(2);
        assertThat(assistant.getToolCalls().stream().map(AssistantMessage.ToolCall::name))
                .containsExactly("tool_a", "tool_b");
        // toolCall id 确定性派生（回放自洽）
        assertThat(assistant.getToolCalls().get(0).id()).isEqualTo("tc-0-0");
        assertThat(assistant.getToolCalls().get(1).id()).isEqualTo("tc-0-1");
    }

    @Test
    void recordThenReplayRoundTripMatches() {
        // 「真实」模型用替身扮演，录制其会话
        ScriptedChatModel underlying = new ScriptedChatModel();
        underlying.enqueue(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "get_order_status", "{\"orderId\":\"O-1\"}")))
                .build());
        underlying.enqueueText("已定位：网关超时");
        RecordingChatModel recorder = RecordingChatModel.of(underlying);

        Prompt first = new Prompt(List.of(new UserMessage("查订单")));
        Prompt second = new Prompt(List.of(
                new UserMessage("查订单"),
                assistantOf(recorder.call(first)),
                ToolResponseMessage.builder().responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call-1", "get_order_status", "{\"status\":\"DELAYED\"}"))).build()));
        assertThat(textOf(recorder.call(second))).isEqualTo("已定位：网关超时");
        assertThat(recorder.recordedCount()).isEqualTo(2);

        // 磁盘往返：写 fixture → 读回 → 严格回放同一会话
        Path file = Path.of("target/recordings/fake-chat-model-roundtrip-test.json");
        recorder.writeTo(file);
        FakeChatModel replay = FakeChatModel.fromRecording(RecordingFixture.load(file));

        assertThat(replay.call(first).getResult().getOutput().getToolCalls())
                .extracting(AssistantMessage.ToolCall::name)
                .containsExactly("get_order_status");
        assertThat(textOf(replay.call(second))).isEqualTo("已定位：网关超时");
        assertThat(replay.callCount()).isEqualTo(2);
    }

    @Test
    void replayMismatchFailsLoudInsteadOfSilentDrift() {
        RecordingFixture fixture = new RecordingFixture(List.of(
                new RecordingFixture.Exchange(
                        RecordingFixture.RequestFingerprint.from(
                                new Prompt(List.of(new UserMessage("原始问题")))),
                        new RecordingFixture.ResponseSpec("答案", List.of()))));
        FakeChatModel replay = FakeChatModel.fromRecording(fixture);

        // 漂移会话（多一条消息）：结构指纹失配 → AssertionError（防静默漏断言）
        Prompt drifted = new Prompt(List.of(new UserMessage("别的"), new UserMessage("原始问题")));
        assertThatThrownBy(() -> replay.call(drifted))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("回放失配");
    }

    @Test
    void replayBeyondRecordingFails() {
        RecordingFixture fixture = new RecordingFixture(List.of(
                new RecordingFixture.Exchange(
                        RecordingFixture.RequestFingerprint.from(new Prompt(List.of(new UserMessage("q")))),
                        new RecordingFixture.ResponseSpec("仅一步", List.of()))));
        FakeChatModel replay = FakeChatModel.fromRecording(fixture);

        replay.call(new Prompt(List.of(new UserMessage("q"))));
        assertThatThrownBy(() -> replay.call(new Prompt(List.of(new UserMessage("q")))))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("回放超录");
    }

    private static String textOf(ChatResponse response) {
        return response.getResult().getOutput().getText();
    }

    private static AssistantMessage assistantOf(ChatResponse response) {
        return response.getResult().getOutput();
    }
}
