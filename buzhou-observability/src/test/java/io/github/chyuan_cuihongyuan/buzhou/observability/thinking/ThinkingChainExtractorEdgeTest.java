package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ThinkingChainExtractor 构造与边缘分支补测（K 会话 R11 / spec 1210 / T1829——R7 逐类
 * 分支数据精定制导）：extraKeys 过滤（null/blank/去重）、maxChars 下限钳制、blank 思维链
 * 值跳到下一 key、ATTR_OMITTED 字符串 "true" 形态。
 * 主适配表由既有 ThinkingChainExtractorTest 覆盖，本类只补边缘分支。
 */
class ThinkingChainExtractorEdgeTest {

    /** 元数据注入沿既有测试先例：AssistantMessage.builder().properties()。 */
    private static AssistantMessage msg(Map<String, Object> metadata) {
        return AssistantMessage.builder().content("text").properties(metadata).build();
    }

    @Test
    void extraKeysNullListIsAccepted() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(null, 100);

        assertThat(extractor.extract(msg(Map.of("reasoningContent", "思考")))
                .orElseThrow().providerKey()).isEqualTo("reasoningContent");
    }

    @Test
    void extraKeysFilterBlankNullAndDuplicates() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(
                Arrays.asList(null, "  ", "custom", "custom", "reasoningContent"), 100);

        // 自定义 key 生效
        assertThat(extractor.extract(msg(Map.of("custom", "c1")))).isPresent();
        // 与内置重复的 custom 顺序仍在内置之后：reasoningContent 优先
        assertThat(extractor.extract(msg(Map.of("custom", "c1", "reasoningContent", "r1")))
                .orElseThrow().providerKey()).isEqualTo("reasoningContent");
    }

    @Test
    void maxCharsClampedToAtLeastOne() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(null, 0);

        var thinking = extractor.extract(msg(Map.of("thinking", "abc"))).orElseThrow();
        assertThat(thinking.content()).hasSize(1); // maxChars 钳制为 1
        assertThat(thinking.truncated()).isTrue();
        assertThat(thinking.originalLength()).isEqualTo(3);
    }

    @Test
    void blankThinkingValueSkipsToNextKey() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(null, 100);

        var thinking = extractor.extract(msg(Map.of(
                "reasoningContent", "   ", // blank：跳过
                "thinking", "ollama 思维链"))).orElseThrow();

        assertThat(thinking.providerKey()).isEqualTo("thinking");
        assertThat(thinking.content()).isEqualTo("ollama 思维链");
    }

    @Test
    void omittedAsStringTrueStillDetected() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(null, 100);

        // Anthropic display=OMITTED 的字符串形态（非 Boolean）
        var thinking = extractor.extract(msg(Map.of(
                "thinking_omitted", "true",
                "reasoning_signature", "sig-1"))).orElseThrow();

        assertThat(thinking.content()).isEmpty();
        assertThat(thinking.omitted()).isTrue();
        assertThat(thinking.signature()).isEqualTo("sig-1");
    }

    @Test
    void nonStringMetadataValueIsIgnored() {
        ThinkingChainExtractor extractor = new ThinkingChainExtractor(null, 100);

        // reasoningContent 为非 String（Integer）→ stringOf 返回 null → 跳到下一 key
        var thinking = extractor.extract(msg(Map.of(
                "reasoningContent", 42,
                "thinking", "真实思维链"))).orElseThrow();

        assertThat(thinking.providerKey()).isEqualTo("thinking");
    }
}
