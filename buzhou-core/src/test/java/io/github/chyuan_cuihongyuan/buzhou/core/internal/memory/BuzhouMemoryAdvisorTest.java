package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BuzhouMemoryAdvisor 分支补测（K 会话 R21 / spec 1220 / T1849——R7 逐类分支数据精定制导）：
 * 写路径 fence（fence 先于落库、异常阻断零写入）、会话去重（Identity 实例语义）、
 * USER/ToolResponse 写入与 ASSISTANT 过滤、conversationId 缺失直通、after 落 assistant。
 * 先例：InMemoryMessageStore（同包真件复用，无 Mockito）。
 */
class BuzhouMemoryAdvisorTest {

    private InMemoryMessageStore store;
    private BuzhouChatMemory memory;
    private AtomicInteger fenceRuns;
    private RuntimeException fenceThrow;
    private BuzhouMemoryAdvisor advisor;

    @BeforeEach
    void setUp() {
        store = new InMemoryMessageStore();
        memory = new BuzhouChatMemory(store);
        fenceRuns = new AtomicInteger();
        fenceThrow = null;
        rebuildAdvisor();
    }

    private void rebuildAdvisor() {
        advisor = new BuzhouMemoryAdvisor(memory, () -> {
            fenceRuns.incrementAndGet();
            if (fenceThrow != null) {
                throw fenceThrow;
            }
        });
    }

    private ChatClientRequest request(Message... messages) {
        return new ChatClientRequest(new Prompt(new ArrayList<>(List.of(messages))),
                new HashMap<>(Map.of(ChatMemory.CONVERSATION_ID, "c1")));
    }

    private ChatClientRequest requestWithoutConversation() {
        return new ChatClientRequest(new Prompt(new ArrayList<>(List.of(new UserMessage("hi")))),
                new HashMap<>());
    }

    @Test
    void beforeWithoutConversationIdReturnsRequestUntouched() {
        ChatClientRequest req = requestWithoutConversation();

        ChatClientRequest out = advisor.before(req, null);

        assertThat(out).isSameAs(req);
        assertThat(memory.get("c1")).isEmpty();
    }

    @Test
    void beforeWritesUserAndToolMessagesAndRebuildsPrompt() {
        UserMessage user = new UserMessage("hi");
        ToolResponseMessage tool = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("id1", "read_file", "内容")))
                .build();

        ChatClientRequest out = advisor.before(request(user, tool), null);

        List<Message> stored = memory.get("c1");
        assertThat(stored).hasSize(2);
        assertThat(out.prompt().getInstructions()).containsExactlyElementsOf(stored);
        assertThat(fenceRuns.get()).isEqualTo(1); // 两类写入共一次 fence
    }

    @Test
    void beforeSkipsAssistantMessagesInInstructions() {
        ChatClientRequest req = request(new UserMessage("hi"),
                new AssistantMessage("不应写入"));

        advisor.before(req, null);

        assertThat(memory.get("c1")).anyMatch(m -> m instanceof UserMessage);
        assertThat(memory.get("c1")).noneMatch(m -> m instanceof AssistantMessage);
    }

    @Test
    void duplicateInstanceIsNotRewritten() {
        UserMessage user = new UserMessage("hi");

        advisor.before(request(user), null);
        int afterFirst = memory.get("c1").size();
        advisor.before(request(user), null); // 同实例再入：seen 去重 → 不重复写

        assertThat(memory.get("c1")).hasSize(afterFirst);
    }

    @Test
    void fenceRunsBeforeWriteAndThrowingFenceBlocksWrite() {
        fenceThrow = new IllegalStateException("lease lost");
        UserMessage user = new UserMessage("hi");

        assertThatThrownBy(() -> advisor.before(request(user), null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(memory.get("c1")).isEmpty(); // 双主窗口零写入

        // fence 恢复后写入照常
        fenceThrow = null;
        rebuildAdvisor();
        advisor.before(request(user), null);
        assertThat(memory.get("c1")).isNotEmpty();
    }

    @Test
    void afterPersistsAssistantMessages() {
        ChatClientResponse response = new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage("回复内容")))),
                new HashMap<>(Map.of(ChatMemory.CONVERSATION_ID, "c1")));

        ChatClientResponse out = advisor.after(response, null);

        assertThat(out).isSameAs(response);
        assertThat(memory.get("c1")).anyMatch(m -> m instanceof AssistantMessage
                && m.getText().equals("回复内容"));
        assertThat(fenceRuns.get()).isEqualTo(1);
    }

    @Test
    void afterWithNullChatResponseOrMissingConversationIsNoOp() {
        assertThat(advisor.after(new ChatClientResponse(null,
                new HashMap<>(Map.of(ChatMemory.CONVERSATION_ID, "c1"))), null)).isNotNull();
        assertThat(advisor.after(new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage("x")))), new HashMap<>()),
                null)).isNotNull();

        assertThat(memory.get("c1")).isEmpty();
        assertThat(fenceRuns.get()).isZero();
    }

    @Test
    void noFenceMeansWriteProceeds() {
        BuzhouMemoryAdvisor noFenceAdvisor = new BuzhouMemoryAdvisor(memory);

        noFenceAdvisor.before(request(new UserMessage("hi")), null);

        assertThat(memory.get("c1")).isNotEmpty(); // null = 无租约语义路径（Buzhou.enhance），行为不变
    }
}
