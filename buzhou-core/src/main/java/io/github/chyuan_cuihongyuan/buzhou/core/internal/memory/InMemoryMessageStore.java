package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消息内存实现（事实台账 = noeviction 语义）。
 *
 * <p>impl-36 / spec 13 §growth-8：有界化——新会话写入超过 {@code maxSessions} 或单会话
 * 消息数超过 {@code maxMessagesPerSession} 时抛 {@link QuotaExceededException}
 * （原子拒绝：不部分写入），绝不静默丢；会话数据经 {@link #deleteSession} 移除后回落。
 */
public class InMemoryMessageStore implements MessageStore {

    /** impl-36：准入串行化（新会话准入 + per-session 上限检查与写入同临界区——原子拒绝）。 */
    private final Object admissionLock = new Object();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<BuzhouMessage>> bySession =
            new ConcurrentHashMap<>();
    private final int maxSessions;
    private final int maxMessagesPerSession;

    public InMemoryMessageStore() {
        this(InMemoryStoreConfig.defaults());
    }

    public InMemoryMessageStore(InMemoryStoreConfig config) {
        InMemoryStoreConfig effective = config == null ? InMemoryStoreConfig.defaults() : config;
        this.maxSessions = effective.maxSessions();
        this.maxMessagesPerSession = effective.maxMessagesPerSession();
    }

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        synchronized (admissionLock) {
            // 事实台账准入（noeviction）：新会话 + 已满 → 拒绝（检查与写入同临界区，原子不留半份）
            if (!bySession.containsKey(sessionId) && bySession.size() >= maxSessions) {
                throw new QuotaExceededException(
                        "内存消息存储会话数已达上限 maxSessions=%d（sessionId=%s）：释放会话或提升配额"
                                .formatted(maxSessions, sessionId));
            }
            CopyOnWriteArrayList<BuzhouMessage> existing = bySession.computeIfAbsent(sessionId,
                    k -> new CopyOnWriteArrayList<>());
            if (existing.size() + messages.size() > maxMessagesPerSession) {
                throw new QuotaExceededException(
                        "会话消息数将超上限 maxMessagesPerSession=%d（sessionId=%s，当前 %d 条，拟追加 %d 条）"
                                .formatted(maxMessagesPerSession, sessionId, existing.size(), messages.size()));
            }
            existing.addAll(messages);
        }
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

    /** impl-36：在册会话数（测试与运维可观测）。 */
    int sessionCount() {
        return bySession.size();
    }

    /** impl-36：单会话消息条数（测试与运维可观测）。 */
    int messageCount(String sessionId) {
        List<BuzhouMessage> messages = bySession.get(sessionId);
        return messages == null ? 0 : messages.size();
    }
}
