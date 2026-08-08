package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Redis {@link MessageStore}：会话消息存 LIST（按写入序）+ 按 id 存 STRING 索引；
 * load 在内存按 (turnSeq, seqInTurn) 排序返回（对齐 JDBC ORDER BY，覆盖乱序 append）。
 */
public class RedisMessageStore implements MessageStore {

    private final RedisSync sync;
    private final RedisKeys keys;

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
        for (String json : raw) {
            BuzhouMessage m = RedisJson.read(json, BuzhouMessage.class);
            if (m != null) {
                msgs.add(m);
            }
        }
        msgs.sort(Comparator.comparingInt(BuzhouMessage::turnSeq)
                .thenComparingInt(BuzhouMessage::seqInTurn));
        return msgs;
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        return Optional.ofNullable(RedisJson.read(sync.commands().get(keys.messageById(messageId)),
                BuzhouMessage.class));
    }
}
