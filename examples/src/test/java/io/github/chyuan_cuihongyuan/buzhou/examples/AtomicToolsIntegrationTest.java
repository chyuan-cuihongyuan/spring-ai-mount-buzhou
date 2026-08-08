package io.github.chyuan_cuihongyuan.buzhou.examples;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 原子工具跨机制集成测试（ticket 16 验收：危险工具 opt-in 后挂 HITL 守卫；
 * todo 跨实例续接经 Attachment 注入）。
 *
 * <p>贯穿 tools（开关矩阵/沙箱/todo state）→ guard（HITL 阻断—授权—放行）→
 * core（装配）→ memory（Attachment 注入视图）四模块，故落 examples 聚合侧。
 */
class AtomicToolsIntegrationTest {

    @TempDir
    Path sandboxRoot;

    @Test
    void dangerousToolBlockedThenApprovedThenRuns() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        // opt-in 开启 run_command，并把危险名单注册进 HITL 守卫（装配侧接线）
        ToolsModule tools = ToolsModule.builder(stores.sessionStateStore())
                .sandboxRoot(sandboxRoot)
                .runCommandEnabled(true)
                .build();
        GuardModule.Builder guardBuilder = GuardModule.builder(stores);
        tools.enabledDangerousToolNames().forEach(name ->
                guardBuilder.dangerousTool(name, "confirm_" + name, "即将执行 ${command}"));
        GuardModule guard = guardBuilder.build();

        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(tools.configure(), guard.configure()));

        // 轮 1：模型调 run_command → 守卫 BLOCK（未授权）
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "run_command", "{\"command\":\"echo guarded-ok\"}")))
                .build());
        model.enqueue(new AssistantMessage("首轮结束"));

        AgentSession session = runtime.spawn("app", "agent", "sess-hitl");
        session.chat("跑个命令");

        Prompt secondCall = model.seenPrompts.get(1);
        assertThat(secondCall.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage && contains(m, "等待人工确认"));

        // 用户授权（业务侧 REST 等效调用）→ 重发同一输入 → 放行执行
        guard.authApi().approve("sess-hitl", "run_command",
                Map.of("command", "echo guarded-ok"), "approve", null);
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-2", "function", "run_command", "{\"command\":\"echo guarded-ok\"}")))
                .build());
        model.enqueue(new AssistantMessage("执行完成"));
        session.chat("重发：跑个命令");
        session.close();

        Prompt fourthCall = model.seenPrompts.get(3);
        assertThat(fourthCall.getInstructions())
                .anyMatch(m -> m instanceof ToolResponseMessage && contains(m, "guarded-ok"));
    }

    @Test
    void todoAttachmentRenderedAfterSessionResume() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();

        ToolsModule tools = ToolsModule.builder(stores.sessionStateStore()).build();
        RuntimeConfig config = RuntimeConfig.merge(
                tools.configure(),
                MemoryModule.configure(Map.of(), stores, model, model,
                        tools.todoAttachmentRenderer(), null));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        // 会话第一段：模型 upsert 一条 todo 后结束
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "tc-1", "function", "todo",
                        "{\"action\":\"upsert\",\"items\":[{\"id\":\"t1\",\"content\":\"续接验证任务\",\"status\":\"in_progress\"}]}")))
                .build());
        model.enqueue(new AssistantMessage("已记录"));
        AgentSession first = runtime.spawn("app", "agent", "sess-resume");
        first.chat("记个任务");
        first.close();

        // 「跨实例续接」模拟：同一 stores（state 持久）、全新 runtime/session 凭同 sessionId 加载
        AgentRuntime runtimeB = Buzhou.runtime(model, stores, config);
        model.enqueue(new AssistantMessage("继续"));
        AgentSession resumed = runtimeB.spawn("app", "agent", "sess-resume");
        resumed.chat("继续");
        resumed.close();

        // 续接后首轮 prompt 注入 todo 清单（system-reminder Attachment 通道）
        Prompt resumedFirstCall = model.seenPrompts.get(2);
        assertThat(resumedFirstCall.getInstructions())
                .anyMatch(m -> contains(m, "任务清单") && contains(m, "续接验证任务"));
    }

    private static boolean contains(Message m, String text) {
        if (m instanceof ToolResponseMessage trm) {
            return trm.getResponses().stream()
                    .anyMatch(r -> r.responseData() != null && r.responseData().contains(text));
        }
        return m.getText() != null && m.getText().contains(text);
    }

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            ChatResponse next = script.poll();
            if (next == null) {
                next = new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
            }
            return next;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
