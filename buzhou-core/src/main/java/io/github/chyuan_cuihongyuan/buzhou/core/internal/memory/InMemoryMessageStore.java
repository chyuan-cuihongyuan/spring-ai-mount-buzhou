package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryMessageStore implements MessageStore {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<BuzhouMessage>> bySession =
            new ConcurrentHashMap<>();

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        bySession.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).addAll(messages);
    }

    @Override
    public List<BuzhouMessage> load(String sessionId) {
        List<BuzhouMessage> messages = bySession.getOrDefault(sessionId, new CopyOnWriteArrayList<>());
        return messages.stream()
                .sorted(Comparator.comparingInt(BuzhouMessage::turnSeq)
                        .thenComparingInt(BuzhouMessage::seqInTurn))
                .toList();
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        return bySession.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.id().equals(messageId))
                .findFirst();
    }

    /** impl-35 / spec 13 §stores-6：移除该会话全部消息（幂等）。 */
    @Override
    public void deleteSession(String sessionId) {
        bySession.remove(sessionId);
    }
}
