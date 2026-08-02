package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryIntegrationTest {

    private List<BuzhouMessage> oldToolSession(String sessionId) {
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= 10; turn++) {
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0,
                    Role.USER, "q" + turn, List.of(), null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "",
                    List.of(new ToolCallRecord("tc-" + turn, "query", "{}")), null, null, null,
                    Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, "x".repeat(3000), List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "query"), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 3,
                    Role.ASSISTANT, "a" + turn, List.of(), null, null, null, Map.of(), Instant.now()));
        }
        return history;
    }

    @Test
    void injectedViewContainsPlaceholdersAndEvidenceToolIsRegistered() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "mem-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, oldToolSession(sessionId));

        List<String> prompts = new CopyOnWriteArrayList<>();
        List<String> toolNames = new CopyOnWriteArrayList<>();
        ChatModel observing = new ChatModel() {
            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }

            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt.getInstructions().toString());
                if (prompt.getOptions() instanceof org.springframework.ai.model.tool.ToolCallingChatOptions t) {
                    t.getToolCallbacks().forEach(cb -> toolNames.add(cb.getToolDefinition().name()));
                }
                return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }
        };

        RuntimeConfig config = MemoryModule.configure(Map.of(), stores.messageStore());
        AgentRuntime runtime = Buzhou.runtime(observing, stores, config);
        AgentSession session = runtime.spawn("app", "agent", sessionId);
        session.chat("继续");

        String injected = prompts.getFirst();
        assertThat(injected).contains("旧工具结果已清理");
        assertThat(injected).contains("evidence-id=");
        long placeholders = injected.split("旧工具结果已清理", -1).length - 1;
        assertThat(placeholders).isGreaterThanOrEqualTo(5);
        assertThat(injected).contains("responseData=" + "x".repeat(3000));
        assertThat(toolNames).contains("read_evidence");
        session.close();
    }
}
