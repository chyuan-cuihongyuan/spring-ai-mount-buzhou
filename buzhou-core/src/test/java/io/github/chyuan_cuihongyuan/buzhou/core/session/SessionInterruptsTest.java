package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionInterrupts 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>手写内存 {@link MessageStore} fake（仓库无 Mockito 惯例），断言 pending 推导、
 * resumeWith 精确注入与幂等无操作。
 */
class SessionInterruptsTest {

    private static final String SESSION = "s-interrupts";

    /** 内存 MessageStore fake：load 返回历史副本，append 追加并记录调用次数。 */
    private static final class InMemoryMessageStore implements MessageStore {
        private final List<BuzhouMessage> history = new ArrayList<>();
        private final List<List<BuzhouMessage>> appended = new ArrayList<>();

        @Override
        public void append(String sessionId, List<BuzhouMessage> messages) {
            appended.add(messages);
            history.addAll(messages);
        }

        @Override
        public List<BuzhouMessage> load(String sessionId) {
            return List.copyOf(history);
        }

        @Override
        public Optional<BuzhouMessage> findById(String messageId) {
            return history.stream().filter(m -> m.id().equals(messageId)).findFirst();
        }
    }

    private static BuzhouMessage assistantWithToolCalls(ToolCallRecord... calls) {
        return new BuzhouMessage("m-1", SESSION, 2, 1, Role.ASSISTANT, "", List.of(calls),
                null, null, null, Map.of(), Instant.now());
    }

    private static BuzhouMessage toolResponse(String toolCallId, String text) {
        return new BuzhouMessage("m-" + toolCallId, SESSION, 2, 2, Role.TOOL, text,
                List.of(), toolCallId, null, null, Map.of(), Instant.now());
    }

    @Test
    void pendingListsToolCallsWithoutMatchingToolResponse() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        store.append(SESSION, List.of(assistantWithToolCalls(
                new ToolCallRecord("tc-1", "web_search", "{}"),
                new ToolCallRecord("tc-2", "calc", "{\"x\":1}"))));
        store.append(SESSION, List.of(toolResponse("tc-1", "已应答")));

        List<SessionInterrupts.PendingToolCall> pending =
                SessionInterrupts.pending(store, SESSION);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().toolCallId()).isEqualTo("tc-2");
        assertThat(pending.getFirst().toolName()).isEqualTo("calc");
        assertThat(pending.getFirst().turn()).isEqualTo(2);
    }

    @Test
    void pendingEmptyWhenAllAnsweredOrNoToolCalls() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        assertThat(SessionInterrupts.pending(store, SESSION)).isEmpty();

        store.append(SESSION, List.of(assistantWithToolCalls(
                new ToolCallRecord("tc-1", "web_search", "{}"))));
        store.append(SESSION, List.of(toolResponse("tc-1", "ok")));
        assertThat(SessionInterrupts.pending(store, SESSION)).isEmpty();
    }

    @Test
    void resumeWithInjectsExactToolResponse() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        store.append(SESSION, List.of(assistantWithToolCalls(
                new ToolCallRecord("tc-9", "human_gate", "{}"))));
        store.appended.clear();

        assertThat(SessionInterrupts.resumeWith(store, SESSION, "tc-9", "批准")).isTrue();

        assertThat(store.appended).hasSize(1);
        BuzhouMessage injected = store.appended.getFirst().getFirst();
        assertThat(injected.role()).isEqualTo(Role.TOOL);
        assertThat(injected.toolCallId()).isEqualTo("tc-9");
        assertThat(injected.content()).isEqualTo("批准");
        assertThat(injected.metadata()).containsEntry("resumed", true)
                .containsEntry("toolName", "human_gate");
        // 注入后不再挂起
        assertThat(SessionInterrupts.pending(store, SESSION)).isEmpty();
    }

    @Test
    void resumeWithUnknownOrAnsweredIdIsNoop() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        store.append(SESSION, List.of(assistantWithToolCalls(
                new ToolCallRecord("tc-1", "web_search", "{}"))));
        store.append(SESSION, List.of(toolResponse("tc-1", "ok")));
        store.appended.clear();

        assertThat(SessionInterrupts.resumeWith(store, SESSION, "tc-1", "重复")).isFalse();
        assertThat(SessionInterrupts.resumeWith(store, SESSION, "tc-404", "未知")).isFalse();
        assertThat(store.appended).isEmpty();
    }

    @Test
    void resumeWithNullResultTextStoresEmptyString() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        store.append(SESSION, List.of(assistantWithToolCalls(
                new ToolCallRecord("tc-1", "web_search", "{}"))));

        assertThat(SessionInterrupts.resumeWith(store, SESSION, "tc-1", null)).isTrue();
        assertThat(store.appended.getFirst().getFirst().content()).isEmpty();
    }
}
