package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.List;
import java.util.Optional;

public interface MessageStore {

    void append(String sessionId, List<BuzhouMessage> messages);

    List<BuzhouMessage> load(String sessionId);

    Optional<BuzhouMessage> findById(String messageId);
}
