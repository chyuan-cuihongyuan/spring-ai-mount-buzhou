package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class HookEndToEndTest {

    static class ScriptedChatModel implements ChatModel {
        final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse next = script.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static ToolCallback tool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    private AgentRuntime runtime(ScriptedChatModel model, List<BuzhouHook> hooks,
                                 ToolCallback... tools) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        return Buzhou.runtime(model, stores, hooks, Set.of(), tools);
    }

    @Test
    void beforeToolBlockReturnsReasonAsToolResult() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "drop_table"));
        model.enqueue(new AssistantMessage("已放弃该操作"));
        BuzhouHook guard = new BuzhouHook() {
            @Override
            public HookResult beforeTool(ToolCallContext ctx) {
                return HookResult.block("危险操作被拒绝");
            }
        };
        AgentRuntime runtime = runtime(model, List.of(guard), tool("drop_table", "done"));
        AgentSession session = runtime.spawn("app", "agent", "s-block");

        String reply = session.chat("删表");
        assertThat(reply).isEqualTo("已放弃该操作");
        session.close();
    }

    @Test
    void afterToolReplaceModifiesResultBeforeModelSeesIt() {
        ScriptedChatModel model = new ScriptedChatModel();
        List<String> seenByModel = new CopyOnWriteArrayList<>();
        ChatModel observing = new ChatModel() {
            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }

            @Override
            public ChatResponse call(Prompt prompt) {
                seenByModel.add(prompt.getInstructions().toString());
                return model.call(prompt);
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }
        };
        model.enqueue(toolCall("tc-1", "read_file"));
        model.enqueue(new AssistantMessage("done"));
        BuzhouHook offloader = new BuzhouHook() {
            @Override
            public HookResult afterTool(ToolCallContext ctx) {
                return HookResult.replace("<offloaded to spill://x>");
            }
        };
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(observing, stores, List.of(offloader), Set.of(),
                tool("read_file", "5万字原文"));
        AgentSession session = runtime.spawn("app", "agent", "s-replace");
        session.chat("读文件");

        assertThat(seenByModel.get(1)).contains("<offloaded to spill://x>")
                .doesNotContain("5万字原文");
        session.close();
    }

    @Test
    void twoToolCallsInOneTurnEachPassChainWithOwnContext() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-1", "function", "tool_a", "{}"),
                        new AssistantMessage.ToolCall("tc-2", "function", "tool_b", "{}")))
                .build());
        model.enqueue(new AssistantMessage("both done"));
        List<String> seen = new CopyOnWriteArrayList<>();
        BuzhouHook recorder = new BuzhouHook() {
            @Override
            public HookResult beforeTool(ToolCallContext ctx) {
                seen.add("before:" + ctx.toolName() + ":" + ctx.toolCallId());
                return HookResult.CONTINUE;
            }

            @Override
            public HookResult afterTool(ToolCallContext ctx) {
                seen.add("after:" + ctx.toolName() + ":" + ctx.result());
                return HookResult.CONTINUE;
            }
        };
        AgentRuntime runtime = runtime(model, List.of(recorder),
                tool("tool_a", "ra"), tool("tool_b", "rb"));
        AgentSession session = runtime.spawn("app", "agent", "s-multi");

        session.chat("do both");

        assertThat(seen).containsExactly(
                "before:tool_a:" + seen.get(0).split(":")[2],
                "after:tool_a:ra",
                "before:tool_b:" + seen.get(2).split(":")[2],
                "after:tool_b:rb");
        assertThat(seen.get(0).split(":")[2]).isNotEqualTo(seen.get(2).split(":")[2]);
        session.close();
    }

    @Test
    void beforeTurnBlockReturnsReasonDirectly() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouHook gate = new BuzhouHook() {
            @Override
            public HookResult beforeTurn(TurnContext ctx) {
                return HookResult.block("会话已被人工暂停");
            }
        };
        AgentRuntime runtime = runtime(model, List.of(gate));
        AgentSession session = runtime.spawn("app", "agent", "s-turn");

        String reply = session.chat("anything");
        assertThat(reply).isEqualTo("会话已被人工暂停");
        session.close();
    }
}
