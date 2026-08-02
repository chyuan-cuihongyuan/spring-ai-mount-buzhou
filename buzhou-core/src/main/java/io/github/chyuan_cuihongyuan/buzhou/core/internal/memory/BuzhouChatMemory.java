package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BuzhouChatMemory implements ChatMemory {

    private final MessageStore messageStore;
    private final ConcurrentHashMap<String, AtomicInteger> turnByConversation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> seqByConversation = new ConcurrentHashMap<>();

    public BuzhouChatMemory(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        AtomicInteger turn = turnByConversation.computeIfAbsent(conversationId,
                k -> new AtomicInteger(seedTurn(conversationId)));
        AtomicInteger seq = seqByConversation.computeIfAbsent(conversationId,
                k -> new AtomicInteger(seedSeq(conversationId)));
        List<BuzhouMessage> converted = new ArrayList<>();
        for (Message message : messages) {
            Role role = roleOf(message);
            if (role == Role.USER) {
                turn.incrementAndGet();
                seq.set(0);
            }
            if (turn.get() == 0) {
                turn.incrementAndGet();
            }
            int turnSeq = turn.get();
            int seqInTurn = seq.incrementAndGet();
            converted.addAll(toBuzhouMessages(message, conversationId, turnSeq, seqInTurn));
        }
        messageStore.append(conversationId, converted);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<BuzhouMessage> stored = messageStore.load(conversationId);
        List<Message> result = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> pendingToolResponses = new ArrayList<>();
        for (BuzhouMessage message : stored) {
            if (message.role() == Role.TOOL) {
                pendingToolResponses.add(new ToolResponseMessage.ToolResponse(
                        message.toolCallId(),
                        (String) message.metadata().getOrDefault("toolName", ""),
                        message.content()));
                continue;
            }
            if (!pendingToolResponses.isEmpty()) {
                result.add(ToolResponseMessage.builder().responses(pendingToolResponses).build());
                pendingToolResponses = new ArrayList<>();
            }
            result.add(toSpringMessage(message));
        }
        if (!pendingToolResponses.isEmpty()) {
            result.add(ToolResponseMessage.builder().responses(pendingToolResponses).build());
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        // 持久层只追加（ticket 06）：clear 语义由压缩视图承担，此处不做物理删除。
    }

    private int seedTurn(String conversationId) {
        return (int) messageStore.load(conversationId).stream()
                .filter(m -> m.role() == Role.USER)
                .count();
    }

    private int seedSeq(String conversationId) {
        List<BuzhouMessage> stored = messageStore.load(conversationId);
        int seq = 0;
        for (int i = stored.size() - 1; i >= 0; i--) {
            if (stored.get(i).role() == Role.USER) {
                break;
            }
            seq++;
        }
        return seq;
    }

    private Role roleOf(Message message) {
        if (message instanceof UserMessage) {
            return Role.USER;
        }
        if (message instanceof AssistantMessage) {
            return Role.ASSISTANT;
        }
        if (message instanceof ToolResponseMessage) {
            return Role.TOOL;
        }
        return Role.SYSTEM;
    }

    private List<BuzhouMessage> toBuzhouMessages(Message message, String sessionId, int turnSeq, int seqInTurn) {
        Role role = roleOf(message);
        if (message instanceof ToolResponseMessage toolResponse) {
            List<BuzhouMessage> responses = new ArrayList<>();
            int subSeq = seqInTurn;
            for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                responses.add(new BuzhouMessage(
                        UUID.randomUUID().toString(), sessionId, turnSeq, subSeq++, Role.TOOL,
                        response.responseData(), List.of(), response.id(), null, null,
                        Map.of("toolName", response.name()), Instant.now()));
            }
            return responses;
        }
        List<ToolCallRecord> toolCalls = List.of();
        String reasoning = null;
        if (message instanceof AssistantMessage assistant) {
            toolCalls = assistant.getToolCalls().stream()
                    .map(tc -> new ToolCallRecord(tc.id(), tc.name(), tc.arguments()))
                    .toList();
            Object reasoningContent = assistant.getMetadata().get("reasoningContent");
            reasoning = reasoningContent instanceof String s ? s : null;
        }
        return List.of(new BuzhouMessage(
                UUID.randomUUID().toString(), sessionId, turnSeq, seqInTurn, role,
                message.getText(), toolCalls, null, reasoning, null,
                new HashMap<>(message.getMetadata()), Instant.now()));
    }

    private Message toSpringMessage(BuzhouMessage message) {
        return switch (message.role()) {
            case USER -> new UserMessage(message.content());
            case SYSTEM -> new SystemMessage(message.content());
            case ASSISTANT -> AssistantMessage.builder()
                    .content(message.content())
                    .properties(message.metadata())
                    .toolCalls(message.toolCalls().stream()
                            .map(tc -> new AssistantMessage.ToolCall(tc.id(), "function", tc.name(), tc.arguments()))
                            .toList())
                    .build();
            case TOOL -> throw new IllegalStateException("TOOL messages are grouped by caller");
        };
    }
}
