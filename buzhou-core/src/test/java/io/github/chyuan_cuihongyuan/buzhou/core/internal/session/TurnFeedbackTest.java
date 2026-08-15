package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 47 §B / T173 / impl-142：turn 反馈捕获端到端（Langfuse score 语义收窄）。
 */
class TurnFeedbackTest {

    @Test
    void validFeedbackPersistsAndEmitsEvent() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("第一轮"));
        model.enqueue(new AssistantMessage("第二轮"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        AgentSession session = runtime.spawn("app", "agent", "sess-fb");
        session.chat("问一");
        session.chat("问二");
        Queue<SessionEvent> events = new ConcurrentLinkedQueue<>();
        session.addEventListener(events::add);

        session.rateTurn(2, "boolean", "false", "答非所问", null);
        session.rateTurn(1, "numeric", "4", null, "implicit");
        session.rateTurn(1, "categorical", "good", "顺手标记", "user");

        // 持久化：scanByPrefix 三条，键前缀与轮次归属正确
        Map<String, StateEntry> persisted = stores.sessionStateStore()
                .scanByPrefix("sess-fb", "buzhou.feedback.");
        assertThat(persisted).hasSize(3);
        assertThat(persisted.keySet()).anyMatch(k -> k.startsWith("buzhou.feedback.2."))
                .anyMatch(k -> k.startsWith("buzhou.feedback.1."));
        // producer/createdTurn 归属
        assertThat(persisted.values()).allMatch(e -> "turn-feedback".equals(e.producer()));
        StateEntry turn2 = persisted.entrySet().stream()
                .filter(e -> e.getKey().startsWith("buzhou.feedback.2."))
                .findFirst().orElseThrow().getValue();
        assertThat(turn2.createdTurn()).isEqualTo(2);
        // lossless 编码可解
        assertThat(turn2.value()).contains("type=boolean").contains("value=false")
                .contains("source=user").contains("comment=");

        // 事件：type/value/source 齐备；comment 非空才带
        List<SessionEvent> feedbackEvents = events.stream()
                .filter(e -> "turn.feedback".equals(e.type())).toList();
        assertThat(feedbackEvents).hasSize(3);
        SessionEvent first = feedbackEvents.get(0);
        assertThat(first.payload()).containsEntry("turnSeq", 2)
                .containsEntry("type", "boolean").containsEntry("value", "false")
                .containsEntry("source", "user").containsKey("comment");
        assertThat(feedbackEvents.get(1).payload()).containsEntry("source", "implicit")
                .doesNotContainKey("comment");
        session.close();
    }

    @Test
    void invalidInputsRejected() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("r"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-fb-bad");
        session.chat("问");

        assertThatThrownBy(() -> session.rateTurn(1, "emoji", "5", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boolean | numeric | categorical");
        assertThatThrownBy(() -> session.rateTurn(1, "boolean", "maybe", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("true/false");
        assertThatThrownBy(() -> session.rateTurn(1, "numeric", "3.5颗星", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("可解析为整数");
        assertThatThrownBy(() -> session.rateTurn(1, "categorical", "  ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不可为空");
        assertThatThrownBy(() -> session.rateTurn(1, "numeric", "5", null, "robot"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user | implicit");
        // 未来轮次拒绝（当前 = 1）
        assertThatThrownBy(() -> session.rateTurn(2, "numeric", "5", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超范围");

        // 全部被拒 → 无落库无事件；关闭后拒绝
        session.close();
        assertThatThrownBy(() -> session.rateTurn(1, "numeric", "5", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

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
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
