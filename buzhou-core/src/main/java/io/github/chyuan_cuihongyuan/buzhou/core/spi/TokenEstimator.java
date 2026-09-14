package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * token 估算 SPI——文本/消息序列的 token 数估算（预算闸与压缩触发输入）。
 */
public interface TokenEstimator {

    int estimate(String text);

    int estimateMessages(List<Message> messages);

    String name();
}
