package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ThinkingChainExtractorTest {

    private final ThinkingChainExtractor extractor = new ThinkingChainExtractor(List.of(), 32768);

    @Test
    void extractsReasoningContentKey() {
        Optional<ExtractedThinking> result = extractor.extract(
                assistant("final reply", Map.of("reasoningContent", "step-by-step reasoning")));
        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("step-by-step reasoning");
        assertThat(result.get().providerKey()).isEqualTo("reasoningContent");
        assertThat(result.get().truncated()).isFalse();
    }

    @Test
    void extractsThinkingKeyWhenReasoningContentAbsent() {
        Optional<ExtractedThinking> result = extractor.extract(
                assistant("reply", Map.of("thinking", "native ollama thought")));
        assertThat(result).isPresent();
        assertThat(result.get().providerKey()).isEqualTo("thinking");
        assertThat(result.get().content()).isEqualTo("native ollama thought");
    }

    @Test
    void extractsThinkingContentMistralKey() {
        Optional<ExtractedThinking> result = extractor.extract(assistant("reply",
                Map.of("thinking_content", "mistral thinking")));
        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("mistral thinking");
        assertThat(result.get().providerKey()).isEqualTo("thinking_content");
    }

    @Test
    void mergesMistralReferenceThinkingContent() {
        Optional<ExtractedThinking> result = extractor.extract(assistant("reply",
                Map.of("thinking_content", "mistral thinking",
                        "reference_thinking_content", "reference body")));
        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("mistral thinking\n---\nreference body");
        assertThat(result.get().providerKey()).isEqualTo("thinking_content");
    }

    @Test
    void extractsGoogleThoughts() {
        Optional<ExtractedThinking> result = extractor.extract(assistant("reply",
                Map.of(ThinkingChainExtractor.KEY_GOOGLE_THOUGHTS, "gemini reasoning")));
        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("gemini reasoning");
        assertThat(result.get().providerKey()).isEqualTo(ThinkingChainExtractor.KEY_GOOGLE_THOUGHTS);
    }

    @Test
    void reasoningContentBeatsThinkingWhenBothPresent() {
        assertThat(extractor.extract(assistant("reply",
                Map.of("reasoningContent", "preferred", "thinking", "fallback")))
                .orElseThrow().content()).isEqualTo("preferred");
    }

    @Test
    void anthropicSignaturePreserved() {
        ExtractedThinking result = extractor.extract(assistant("reply",
                Map.of("thinking", "anthropic chain", "reasoning_signature", "sig-123"))).orElseThrow();
        assertThat(result.signature()).isEqualTo("sig-123");
    }

    @Test
    void anthropicOmittedReturnsMetadataOnlyEvent() {
        Optional<ExtractedThinking> result = extractor.extract(assistant("reply",
                Map.of(ThinkingChainExtractor.ATTR_OMITTED, true, "reasoning_signature", "sig")));
        assertThat(result).isPresent();
        assertThat(result.get().omitted()).isTrue();
        assertThat(result.get().content()).isEmpty();
        assertThat(result.get().signature()).isEqualTo("sig");
    }

    @Test
    void officialOpenAiNoThinkingReturnsEmpty() {
        // 官方 OpenAI GPT-5/o1/o3：无推理文本，仅 usage reasoning_tokens
        assertThat(extractor.extract(assistant("reply", Map.of()))).isEmpty();
    }

    @Test
    void extraKeysAppendedAfterBuiltin() {
        ThinkingChainExtractor withExtra = new ThinkingChainExtractor(List.of("vendor_thinking"), 32768);
        assertThat(withExtra.extract(assistant("reply", Map.of("vendor_thinking", "custom chain"))))
                .isPresent();
    }

    @Test
    void truncatesBeyondMaxCharsAndMarksFlag() {
        ThinkingChainExtractor small = new ThinkingChainExtractor(List.of(), 10);
        String longChain = "0123456789ABCDEF"; // 16 chars
        ExtractedThinking result = small.extract(assistant("reply", Map.of("reasoningContent", longChain)))
                .orElseThrow();
        assertThat(result.truncated()).isTrue();
        assertThat(result.originalLength()).isEqualTo(16);
        assertThat(result.content()).hasSize(10).isEqualTo("0123456789");
    }

    @Test
    void nullMessageReturnsEmpty() {
        assertThat(extractor.extract(null)).isEmpty();
    }

    private static AssistantMessage assistant(String content, Map<String, Object> metadata) {
        return AssistantMessage.builder().content(content).properties(metadata).build();
    }
}
