package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.RecordingChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.RecordingFixture;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ToolCallSpec;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-01（spec 12 §core-1）端到端：FakeChatModel 脚本（含<b>并行工具调用块</b>）驱动完整
 * agent session；录制→落盘→严格回放往返；回放失配（会话漂移）即测试失败而非静默。
 */
class FakeChatModelEndToEndTest {

    @Test
    void parallelScriptDrivesBothToolsAndCompletesTurn() {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.parallel(new ToolCallSpec("tool_a", "{}"), new ToolCallSpec("tool_b", "{}")),
                ScriptStep.text("a 与 b 均已完成"));
        FakeModelGuard.requireTestDouble(model);

        List<String> invoked = new CopyOnWriteArrayList<>();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults(),
                fixedTool("tool_a", "result-a", invoked),
                fixedTool("tool_b", "result-b", invoked));

        AgentSession session = runtime.spawn("fake-app", "support-agent", "parallel-session");
        String reply = session.chat("并行查两个数据源");
        session.close();

        // 脚本驱动：并行块的两个工具都被执行、Turn 以脚本终局文本收尾
        assertThat(reply).isEqualTo("a 与 b 均已完成");
        assertThat(invoked).containsExactlyInAnyOrder("tool_a", "tool_b");

        // 第二次模型调用：两条工具结果聚合回注（单条 ToolResponseMessage × 2 response）
        List<Message> secondCall = model.seenPrompts.get(1).getInstructions();
        List<Message> toolResults = secondCall.stream()
                .filter(m -> m instanceof ToolResponseMessage).toList();
        assertThat(toolResults).hasSize(1);
        assertThat(((ToolResponseMessage) toolResults.getFirst()).getResponses()).hasSize(2);
    }

    @Test
    void recordThenReplayReproducesConversationFromDisk() {
        // 「真实」模型用脚本替身扮演；RecordingChatModel 录制其会话
        ScriptedChatModel underlying = new ScriptedChatModel();
        underlying.enqueue(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "c1", "function", "lookup", "{\"key\":\"k\"}"))).build());
        underlying.enqueueText("已根据检索结果给出结论");
        RecordingChatModel recorder = RecordingChatModel.of(underlying);
        FakeModelGuard.requireTestDouble(recorder);

        String recorded = runToolLoopSession(recorder, "record-session");

        // 落盘 → 读回 → 严格回放：同一会话形状产出相同终局
        Path fixture = Path.of("target/recordings/fake-model-e2e-roundtrip.json");
        recorder.writeTo(fixture);
        FakeChatModel replay = FakeChatModel.fromRecording(RecordingFixture.load(fixture));
        FakeModelGuard.requireTestDouble(replay);

        String replayed = runToolLoopSession(replay, "replay-session");

        assertThat(replayed).isEqualTo(recorded);
        assertThat(replay.callCount()).isEqualTo(recorder.recordedCount()).isEqualTo(2);
        // 回放的第二调用确实见到了工具结果（工具调用循环协同，而非跳过）
        Message last = replay.seenPrompts.get(1).getInstructions().getLast();
        assertThat(last).isInstanceOf(ToolResponseMessage.class);
    }

    @Test
    void replayDriftFailsLoudInsteadOfSilentMismatch() {
        ScriptedChatModel underlying = new ScriptedChatModel();
        underlying.enqueueText("直答");
        RecordingChatModel recorder = RecordingChatModel.of(underlying);
        runPlainSession(recorder, "drift-record");

        FakeChatModel replay = FakeChatModel.fromRecording(recorder.snapshot());

        // 漂移：预置了额外历史（消息数变化）→ 结构指纹失配 → AssertionError（防静默漏断言）
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.messageStore().append("drift-session",
                BuzhouDemo.seedHistory("drift-session", 1));
        AgentRuntime runtime = Buzhou.runtime(replay, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("fake-app", "support-agent", "drift-session");
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> session.chat("继续"));
        session.close();
        Throwable root = thrown;
        while (root.getCause() != null && root != root.getCause()) {
            root = root.getCause();
        }
        assertThat(root).isInstanceOf(AssertionError.class);
        assertThat(root.getMessage()).contains("回放失配");
    }

    private static String runToolLoopSession(org.springframework.ai.chat.model.ChatModel model, String sid) {
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults(),
                fixedTool("lookup", "lookup-result", new CopyOnWriteArrayList<>()));
        AgentSession session = runtime.spawn("fake-app", "support-agent", sid);
        String reply = session.chat("检索关键信息");
        session.close();
        return reply;
    }

    private static void runPlainSession(org.springframework.ai.chat.model.ChatModel model, String sid) {
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("fake-app", "support-agent", sid);
        session.chat("打个招呼");
        session.close();
    }

    private static ToolCallback fixedTool(String name, String result, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                sink.add(name);
                return result;
            }
        };
    }
}
