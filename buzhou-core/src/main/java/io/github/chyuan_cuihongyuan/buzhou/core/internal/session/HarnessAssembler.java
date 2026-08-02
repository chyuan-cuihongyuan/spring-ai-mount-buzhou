package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouChatMemory;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

public class HarnessAssembler {

    public AgentSession assemble(String appId, String agentName, String sessionId,
                                 ChatModel chatModel, BuzhouStores stores,
                                 SessionResourceRegistry registry,
                                 Runnable onClose,
                                 ToolCallback... tools) {
        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        ChatClient.Builder builder = chatClientBuilder(chatModel, memory);
        if (tools != null && tools.length > 0) {
            builder.defaultToolCallbacks(java.util.Arrays.asList(tools));
        }
        return new DefaultAgentSession(appId, agentName, sessionId, builder.build(), registry, onClose);
    }

    public ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        return chatClientBuilder(builder, memory);
    }

    private ChatClient.Builder chatClientBuilder(ChatModel chatModel, BuzhouChatMemory memory) {
        return chatClientBuilder(ChatClient.builder(chatModel), memory);
    }

    private ChatClient.Builder chatClientBuilder(ChatClient.Builder builder, BuzhouChatMemory memory) {
        return builder.defaultAdvisors(
                ToolCallingAdvisor.builder().build(),
                new BuzhouMemoryAdvisor(memory));
    }
}
