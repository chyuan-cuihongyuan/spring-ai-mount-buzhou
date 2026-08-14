package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
import java.util.Optional;

public interface MessageStore {

    void append(String sessionId, List<BuzhouMessage> messages);

    List<BuzhouMessage> load(String sessionId);

    Optional<BuzhouMessage> findById(String messageId);

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的全部消息（含按 id 索引）。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }
}
