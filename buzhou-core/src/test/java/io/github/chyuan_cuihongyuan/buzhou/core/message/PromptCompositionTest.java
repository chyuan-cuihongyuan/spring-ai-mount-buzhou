package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 704 / T1008–T1009：提示词角色构成拆解——降序+占比精确、空诚实零、
 * null fail-fast、同值字典序稳定。
 */
class PromptCompositionTest {

    @Test
    void sectionsSortDescendingWithExactShares() {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("s".repeat(200)),
                new UserMessage("u".repeat(300)),
                new AssistantMessage("a".repeat(100)),
                new UserMessage("u2".repeat(0) /* 空文本消息也计数 */),
                toolMessage("t".repeat(500))));
        PromptComposition.Report report = PromptComposition.analyze(prompt);
        assertThat(report.totalChars()).isEqualTo(1100);
        assertThat(report.sections()).hasSize(4);
        assertThat(report.sections().get(0).role()).isEqualTo("TOOL");
        assertThat(report.sections().get(0).chars()).isEqualTo(500);
        assertThat(report.sections().get(0).share()).isEqualTo(500.0 / 1100);
        assertThat(report.sections().get(1).role()).isEqualTo("USER");
        assertThat(report.sections().get(1).messages()).isEqualTo(2);
        assertThat(report.sections().get(1).share()).isEqualTo(300.0 / 1100);
        assertThat(report.sections().get(3).role()).isEqualTo("ASSISTANT");
    }

    private static ToolResponseMessage toolMessage(String text) {
        return ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "query", text)))
                .build();
    }

    @Test
    void emptyPromptAndNullTextsAreHonestZero() {
        PromptComposition.Report empty = PromptComposition.analyze(
                new Prompt(List.of(), ChatOptions.builder().build()));
        assertThat(empty.totalChars()).isZero();
        assertThat(empty.sections()).isEmpty();

        // 全 null 文本场景：自定义 Message getText 返回 null 也计数（chars 不加）
        org.springframework.ai.chat.messages.Message nullText =
                new org.springframework.ai.chat.messages.Message() {
                    @Override
                    public String getText() {
                        return null;
                    }

                    @Override
                    public java.util.Map<String, Object> getMetadata() {
                        return java.util.Map.of();
                    }

                    @Override
                    public org.springframework.ai.chat.messages.MessageType getMessageType() {
                        return org.springframework.ai.chat.messages.MessageType.USER;
                    }
                };
        PromptComposition.Report report = PromptComposition.analyze(new Prompt(List.of(nullText)));
        assertThat(report.totalChars()).isZero();
        assertThat(report.sections().get(0).messages()).isEqualTo(1);
        assertThat(report.sections().get(0).share()).isZero();
    }

    @Test
    void nullPromptFailsFastAndEqualCharsStableOrder() {
        assertThatThrownBy(() -> PromptComposition.analyze(null))
                .isInstanceOf(NullPointerException.class);
        // 同值：角色名字典序稳定
        Prompt tie = new Prompt(List.of(
                new SystemMessage("ab"),
                new UserMessage("cd")));
        PromptComposition.Report report = PromptComposition.analyze(tie);
        assertThat(report.sections().get(0).role()).isEqualTo("SYSTEM"); // 同 chars=2 → 字典序
        assertThat(report.sections().get(1).role()).isEqualTo("USER");
    }
}
