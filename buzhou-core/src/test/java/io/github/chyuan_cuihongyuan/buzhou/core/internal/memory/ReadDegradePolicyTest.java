package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradeHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradePolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 42 §B / T156 / impl-127：消息读失败降级——OFF（默认）原样上抛（行为不变）；
 * EMPTY 降级空历史继续（可感：不静默吞故障）。
 */
class ReadDegradePolicyTest {

    @AfterEach
    void resetPolicy() {
        ReadDegradeHolder.set(ReadDegradePolicy.OFF);
    }

    /** 永远读失败的 store 替身。 */
    private static final MessageStore FAILING = new MessageStore() {
        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            throw new IllegalStateException("存储读失败（模拟 DB 瞬断）");
        }

        @Override
        public java.util.Optional<BuzhouMessage> findById(String id) {
            return java.util.Optional.empty();
        }
    };

    @Test
    void offByDefaultFailsTheReadAsBefore() {
        BuzhouChatMemory memory = new BuzhouChatMemory(FAILING);
        assertThat(ReadDegradeHolder.get()).isEqualTo(ReadDegradePolicy.OFF);
        assertThatThrownBy(() -> memory.get("s1"))
                .hasMessageContaining("存储读失败");
    }

    @Test
    void emptyPolicyDegradesToEmptyHistoryAndKeepsSessionUsable() {
        BuzhouChatMemory memory = new BuzhouChatMemory(FAILING);
        ReadDegradeHolder.set(ReadDegradePolicy.EMPTY);

        // 读失败降级空历史：get 返回空列表（本轮以无历史继续——模型看不到过往）
        assertThat(memory.get("s1")).isEmpty();

        // 写路径不受降级影响：新消息照常 add（append 正常的 store 下轮即可读回）
        MemoryRecorder recorder = new MemoryRecorder();
        BuzhouChatMemory healthy = new BuzhouChatMemory(recorder);
        healthy.add("s1", List.of(new UserMessage("问题"),
                new AssistantMessage("回答")));
        assertThat(healthy.get("s1")).hasSize(2);
    }

    /** 记录写入的内存 store 替身（验证降级不破坏写路径）。 */
    private static final class MemoryRecorder implements MessageStore {
        private final List<BuzhouMessage> messages = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
            this.messages.addAll(messages);
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            return List.copyOf(messages);
        }

        @Override
        public java.util.Optional<BuzhouMessage> findById(String id) {
            return java.util.Optional.empty();
        }
    }
}
