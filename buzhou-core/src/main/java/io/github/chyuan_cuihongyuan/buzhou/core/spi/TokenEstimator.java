package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface TokenEstimator {

    int estimate(String text);

    int estimateMessages(List<Message> messages);

    String name();
}
