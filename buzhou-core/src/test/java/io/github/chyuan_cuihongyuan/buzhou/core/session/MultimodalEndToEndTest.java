package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 多模态输入透传 e2e（spec 27 / T106 / impl-81）：媒体随本轮下发、metadata 持久化、
 * 重发只随最近一条带媒体消息（更早降级文本标记）、token 估算媒体固定计数、入参校验。
 */
class MultimodalEndToEndTest {

    /** 媒体随本轮 UserMessage 下发并持久化到 message store metadata。 */
    @Test
    void mediaReachesModelAndPersistsToStore() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("这是一张山景图");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "mm-1");

        session.chat("看这张图", List.of(MediaRef.of("image/png", "https://cdn.example.com/1.png")));

        UserMessage userMessage = firstUserMessage(model.seenPrompts.get(0));
        assertThat(userMessage.getText()).contains("看这张图");
        assertThat(userMessage.getMedia()).hasSize(1);
        assertThat(String.valueOf(userMessage.getMedia().get(0).getData()))
                .contains("https://cdn.example.com/1.png");

        Object mediaRefs = stores.messageStore().load("mm-1").getFirst().metadata().get("mediaRefs");
        assertThat(mediaRefs).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(1);
        session.close();
    }

    /** 重发策略：第二轮起，媒体只随最近一条带媒体消息；更早的降级为文本标记。 */
    @Test
    void historyReplaysOnlyLatestMedia() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok1");
        model.enqueueText("ok2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "mm-2");

        session.chat("第一张", List.of(MediaRef.of("image/png", "https://cdn.example.com/1.png")));
        session.chat("第二张", List.of(MediaRef.of("image/jpeg", "https://cdn.example.com/2.jpg")));

        List<UserMessage> users = model.seenPrompts.get(1).getInstructions().stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> (UserMessage) m)
                .toList();
        assertThat(users).hasSize(2);
        assertThat(users.getFirst().getMedia()).isEmpty(); // 旧媒体不重发
        assertThat(users.getFirst().getText()).contains("[历史媒体（本轮未随附）]")
                .contains("https://cdn.example.com/1.png");
        assertThat(users.getLast().getMedia()).hasSize(1); // 最近一条随附
        assertThat(String.valueOf(users.getLast().getMedia().get(0).getData()))
                .contains("2.jpg");
        session.close();
    }

    /** 纯文本轮回归：无媒体时 prompt 组装与既有行为零差异。 */
    @Test
    void textOnlyChatUnchanged() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("plain");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "mm-3");

        assertThat(session.chat("纯文本")).isEqualTo("plain");
        assertThat(firstUserMessage(model.seenPrompts.get(0)).getMedia()).isEmpty();
        session.close();
    }

    /** token 估算：媒体按固定档位计（每媒体 320）。 */
    @Test
    void estimatorChargesFixedTokensPerMedia() {
        io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator estimator =
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator();
        UserMessage withMedia = UserMessage.builder().text("hello world")
                .media(new org.springframework.ai.content.Media(
                        org.springframework.util.MimeType.valueOf("image/png"),
                        java.net.URI.create("https://x/1.png")))
                .build();
        UserMessage textOnly = new UserMessage("hello world");
        int charge = estimator.estimateMessages(List.of(withMedia))
                - estimator.estimateMessages(List.of(textOnly));
        assertThat(charge).isEqualTo(MediaRef.TOKENS_PER_MEDIA);
    }

    /** 入参校验：空 mimeType / null uri 构造期拒绝。 */
    @Test
    void mediaRefValidatesInputs() {
        assertThatThrownBy(() -> MediaRef.of("", "https://x/1.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MediaRef("image/png", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static UserMessage firstUserMessage(Prompt prompt) {
        return prompt.getInstructions().stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> (UserMessage) m)
                .findFirst()
                .orElseThrow();
    }
}
