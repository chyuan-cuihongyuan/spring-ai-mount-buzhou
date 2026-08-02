package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSessionSpineTest {

    static class ScriptedChatModel implements ChatModel {
        private final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

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
            if (next == null) {
                next = new ChatResponse(List.of(new Generation(new AssistantMessage("default reply"))));
            }
            return next;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static ToolCallback queryOrderTool() {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name("query_order")
                        .description("查询订单状态")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return "{\"status\":\"shipped\"}";
            }
        };
    }

    private AgentRuntime newRuntime(ScriptedChatModel model, BuzhouStores stores) {
        return Buzhou.runtime(model, stores, queryOrderTool());
    }

    @Test
    void threeTurnConversationIsFullyPersistedIncludingToolCalls() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = newRuntime(model, stores);

        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("tc-1", "function", "query_order", "{}")))
                .build());
        model.enqueue(new AssistantMessage("订单 ORD-1 已发货"));
        model.enqueue(new AssistantMessage("进展：运输中"));
        model.enqueue(new AssistantMessage("已送达"));

        AgentSession session = runtime.spawn("app", "ops-agent", "sess-1");
        String reply1 = session.chat("帮我查订单 ORD-1");
        assertThat(reply1).isEqualTo("订单 ORD-1 已发货");
        session.chat("进展怎样了？");
        session.chat("送到了吗？");

        List<BuzhouMessage> history = stores.messageStore().load("sess-1");
        assertThat(history).hasSizeGreaterThanOrEqualTo(8);
        assertThat(history.stream().filter(m -> m.role() == Role.USER)).hasSize(3);
        assertThat(history.stream().filter(m -> m.role() == Role.TOOL)).hasSize(1);
        BuzhouMessage assistantWithToolCall = history.stream()
                .filter(m -> m.role() == Role.ASSISTANT && !m.toolCalls().isEmpty())
                .findFirst().orElseThrow();
        assertThat(assistantWithToolCall.toolCalls().getFirst().name()).isEqualTo("query_order");
        BuzhouMessage toolResult = history.stream()
                .filter(m -> m.role() == Role.TOOL).findFirst().orElseThrow();
        assertThat(toolResult.toolCallId()).isEqualTo("tc-1");
        assertThat(toolResult.content()).contains("shipped");

        List<org.springframework.ai.chat.messages.Message> view =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory
                        .BuzhouChatMemory(stores.messageStore()).get("sess-1");
        assertThat(view).isNotEmpty();
        session.close();
    }

    @Test
    void secondSpawnOfSameSessionFailsUnlessSteal() {
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = newRuntime(model, Buzhou.inMemoryStores());
        AgentSession first = runtime.spawn("app", "agent", "sess-dup");

        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-dup"))
                .isInstanceOf(SessionAlreadyActiveException.class);

        AgentSession stolen = runtime.spawn("app", "agent", "sess-dup", SpawnOptions.withSteal());
        assertThat(stolen.sessionId()).isEqualTo("sess-dup");
        stolen.close();
        first.close();
    }

    @Test
    void closeIsIdempotentAndBlocksFurtherChat() {
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = newRuntime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-close");
        session.close();
        session.close();

        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closeReleasesLeaseSoSessionCanRespawn() {
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = newRuntime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-re");
        session.close();

        AgentSession respawned = runtime.spawn("app", "agent", "sess-re");
        assertThat(respawned.sessionId()).isEqualTo("sess-re");
        respawned.close();
    }

    @Test
    void closeTriggersRegisteredResources() {
        ScriptedChatModel model = new ScriptedChatModel();
        SessionResourceRegistry registry = new SessionResourceRegistry();
        AtomicBoolean cleaned = new AtomicBoolean();
        registry.register("probe", () -> cleaned.set(true));

        AgentSession session = new HarnessAssembler().assemble(
                "app", "agent", "sess-res", model, Buzhou.inMemoryStores(), registry,
                registry::closeAll, java.util.List.of(), java.util.Set.of(), java.util.Set.of(), null);
        session.close();

        assertThat(cleaned).isTrue();
        assertThat(registry.isClosed()).isTrue();
    }

    @Test
    void streamDeliversChunks() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("chunked reply"));
        AgentRuntime runtime = newRuntime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-stream");

        List<ChatResponse> chunks = session.stream("hi").collectList().block();
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().getResult().getOutput().getText()).isEqualTo("chunked reply");
        session.close();
    }

    @Test
    void enhanceAttachesMemoryToPlainBuilder() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        model.enqueue(new AssistantMessage("enhanced reply"));

        org.springframework.ai.chat.client.ChatClient client = Buzhou.enhance(
                org.springframework.ai.chat.client.ChatClient.builder(model), stores).build();
        String content = client.prompt().user("hello")
                .advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, "sess-enh"))
                .call().content();

        assertThat(content).isEqualTo("enhanced reply");
        assertThat(stores.messageStore().load("sess-enh"))
                .extracting(BuzhouMessage::role)
                .containsExactly(Role.USER, Role.ASSISTANT);
    }

    @Test
    void respawnContinuesTurnSequence() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = newRuntime(model, stores);
        model.enqueue(new AssistantMessage("r1"));
        AgentSession first = runtime.spawn("app", "agent", "sess-turn");
        first.chat("q1");
        first.close();

        model.enqueue(new AssistantMessage("r2"));
        AgentSession second = runtime.spawn("app", "agent", "sess-turn");
        second.chat("q2");
        second.close();

        List<BuzhouMessage> history = stores.messageStore().load("sess-turn");
        assertThat(history).filteredOn(m -> m.role() == Role.USER)
                .extracting(BuzhouMessage::turnSeq)
                .containsExactly(1, 2);
        assertThat(history).extracting(m -> m.turnSeq() + ":" + m.seqInTurn())
                .doesNotHaveDuplicates();
    }
}
