package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BuzhouMemoryAdvisor implements BaseAdvisor {

    private final BuzhouChatMemory memory;
    private final Map<String, Set<Message>> seenByConversation = new ConcurrentHashMap<>();

    public BuzhouMemoryAdvisor(BuzhouChatMemory memory) {
        this.memory = memory;
    }

    @Override
    public String getName() {
        return "BuzhouMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 400;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String conversationId = conversationIdOf(request);
        if (conversationId == null) {
            return request;
        }
        Set<Message> seen = seenSet(conversationId);
        seen.addAll(memory.get(conversationId));

        List<Message> newMessages = request.prompt().getInstructions().stream()
                .filter(m -> !seen.contains(m))
                .filter(m -> m instanceof UserMessage || m instanceof ToolResponseMessage)
                .toList();
        if (!newMessages.isEmpty()) {
            memory.add(conversationId, newMessages);
            seen.addAll(newMessages);
        }

        List<Message> rebuilt = new ArrayList<>(memory.get(conversationId));
        seen.addAll(rebuilt);
        return request.mutate()
                .prompt(new Prompt(rebuilt, request.prompt().getOptions()))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        String conversationId = conversationIdOf(response);
        if (conversationId == null || response.chatResponse() == null) {
            return response;
        }
        List<Message> assistantMessages = response.chatResponse().getResults().stream()
                .map(g -> (Message) g.getOutput())
                .filter(m -> m instanceof AssistantMessage)
                .toList();
        if (!assistantMessages.isEmpty()) {
            memory.add(conversationId, assistantMessages);
            seenSet(conversationId).addAll(assistantMessages);
        }
        return response;
    }

    private Set<Message> seenSet(String conversationId) {
        return seenByConversation.computeIfAbsent(conversationId,
                k -> Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    private String conversationIdOf(ChatClientRequest request) {
        Object value = request.context().get(ChatMemory.CONVERSATION_ID);
        return value instanceof String s ? s : null;
    }

    private String conversationIdOf(ChatClientResponse response) {
        Object value = response.context().get(ChatMemory.CONVERSATION_ID);
        return value instanceof String s ? s : null;
    }
}
