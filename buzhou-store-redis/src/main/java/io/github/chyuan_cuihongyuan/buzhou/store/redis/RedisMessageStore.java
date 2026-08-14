package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouDataCorruptionException;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis {@link MessageStore}：会话消息存 LIST（按写入序）+ 按 id 存 STRING 索引；
 * load 在内存按 (turnSeq, seqInTurn) 排序返回（对齐 JDBC ORDER BY，覆盖乱序 append）。
 *
 * <p>spec 13 §stores-7 / ticket 32 脏数据隔离：逐条解析，单条坏 JSON 只跳过该条
 * （WARN + {@link BuzhouDataCorruptionException} 记录 + {@link #corruptionCount()} 计数），
 * 绝不炸整个会话加载。
 */
public class RedisMessageStore implements MessageStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisMessageStore.class);

    private final RedisSync sync;
    private final RedisKeys keys;

    /** 脏数据计数（跳过不静默：可断言 / 可采集）。 */
    private final AtomicLong corruptionCount = new AtomicLong();

    public RedisMessageStore(RedisSync sync, RedisKeys keys) {
        this.sync = sync;
        this.keys = keys;
    }

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        var c = sync.commands();
        String listKey = keys.messageList(sessionId);
        for (BuzhouMessage m : messages) {
            String json = RedisJson.write(m);
            c.set(keys.messageById(m.id()), json);
            c.rpush(listKey, json);
        }
    }

    @Override
    public List<BuzhouMessage> load(String sessionId) {
        List<String> raw = sync.commands().lrange(keys.messageList(sessionId), 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<BuzhouMessage> msgs = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            parseEntry(sessionId, i, raw.get(i)).ifPresent(msgs::add);
        }
        msgs.sort(Comparator.comparingInt(BuzhouMessage::turnSeq)
                .thenComparingInt(BuzhouMessage::seqInTurn));
        return msgs;
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        String json = sync.commands().get(keys.messageById(messageId));
        return parseEntry("n/a(messageId=" + messageId + ")", 0, json);
    }

    /** 单条解析（坏 JSON → WARN + 损坏计数 + 跳过）。 */
    private Optional<BuzhouMessage> parseEntry(String sessionId, int listIndex, String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(RedisJson.read(json, BuzhouMessage.class));
        } catch (RuntimeException e) {
            BuzhouDataCorruptionException corruption = new BuzhouDataCorruptionException(
                    "消息记录损坏已跳过(sessionId=%s, listIndex=%d)".formatted(sessionId, listIndex), e);
            // 记录 BuzhouDataCorruptionException（含根因栈）：跳过不静默
            LOG.warn(corruption.getMessage(), corruption);
            corruptionCount.incrementAndGet();
            return Optional.empty();
        }
    }

    /** 累计跳过的脏消息条数（丢弃不可静默——测试与运维可断言该计数）。 */
    public long corruptionCount() {
        return corruptionCount.get();
    }
}
