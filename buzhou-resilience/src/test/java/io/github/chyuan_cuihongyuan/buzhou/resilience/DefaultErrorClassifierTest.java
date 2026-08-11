package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultErrorClassifier} 纯函数决策表测试（次缝合点）：按异常 / 响应形态穷举「→ 类别 + retryAfter」。
 * 与经 e2e 枚举相比，表测试对「每种 provider 错误形态 → 期望类别」覆盖更清晰。
 */
class DefaultErrorClassifierTest {

    private final DefaultErrorClassifier classifier = new DefaultErrorClassifier();

    // ---- 限流 ----

    @Test
    void http429WithRetryAfterSecondsClassifiedAsRateLimit() {
        Classification c = classifier.classify(http(429, "5"), null);
        assertThat(c.category()).isEqualTo(ErrorCategory.RATE_LIMIT);
        assertThat(c.retryAfter()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void http429WithoutRetryAfterClassifiedAsRateLimitNoRetryAfter() {
        Classification c = classifier.classify(http(429, null), null);
        assertThat(c.category()).isEqualTo(ErrorCategory.RATE_LIMIT);
        assertThat(c.retryAfter()).isNull();
    }

    // ---- 鉴权 ----

    @Test
    void http401And403ClassifiedAsAuth() {
        assertThat(classifier.classify(http(401, null), null).category()).isEqualTo(ErrorCategory.AUTH);
        assertThat(classifier.classify(http(403, null), null).category()).isEqualTo(ErrorCategory.AUTH);
    }

    // ---- 网络（含 5xx） ----

    @Test
    void http5xxClassifiedAsNetwork() {
        assertThat(classifier.classify(serverHttp(500), null).category()).isEqualTo(ErrorCategory.NETWORK);
        assertThat(classifier.classify(serverHttp(502), null).category()).isEqualTo(ErrorCategory.NETWORK);
        assertThat(classifier.classify(serverHttp(503), null).category()).isEqualTo(ErrorCategory.NETWORK);
    }

    @Test
    void ioExceptionClassifiedAsNetwork() {
        assertThat(classifier.classify(
                new UncheckedIOException(new IOException("connection reset")), null).category())
                .isEqualTo(ErrorCategory.NETWORK);
    }

    @Test
    void restClientResponseExceptionWrappedInCauseStillDetected() {
        // provider 常把 HTTP 异常包在执行异常里：沿因果链须仍能识别 429。
        assertThat(classifier.classify(
                new RuntimeException("call failed", http(429, "2")), null).category())
                .isEqualTo(ErrorCategory.RATE_LIMIT);
    }

    // ---- 内容拒绝（静默通道） ----

    @Test
    void contentFilterFinishReasonClassifiedAsContent() {
        assertThat(classifier.classify(null, responseWithFinishReason("content_filter")).category())
                .isEqualTo(ErrorCategory.CONTENT);
    }

    @Test
    void normalResponseNotContentRefusal() {
        assertThat(classifier.classify(null, responseWithFinishReason("stop")).category())
                .isEqualTo(ErrorCategory.UNKNOWN);
    }

    // ---- 未知 ----

    @Test
    void http404ClassifiedAsUnknown() {
        assertThat(classifier.classify(http(404, null), null).category()).isEqualTo(ErrorCategory.UNKNOWN);
    }

    @Test
    void unrecognizedExceptionClassifiedAsUnknown() {
        assertThat(classifier.classify(new IllegalStateException("weird"), null).category())
                .isEqualTo(ErrorCategory.UNKNOWN);
    }

    @Test
    void nullErrorAndResponseClassifiedAsUnknown() {
        assertThat(classifier.classify(null, null).category()).isEqualTo(ErrorCategory.UNKNOWN);
    }

    /**
     * 02 验收「OpenAI/Anthropic 须随本票交付」：二者经 Spring AI 的 RestClient 传输，抛
     * {@code RestClientResponseException}——无需依赖其私有 SDK 异常类型，同一 HTTP 状态即归入五类。
     * 本测试把该覆盖关系显式固化（OpenAI 429 = RATE_LIMIT、401 = AUTH）。
     */
    @Test
    void openAiAndAnthropicViaRestClientAreCovered() {
        assertThat(classifier.classify(http(429, "2"), null).category()).isEqualTo(ErrorCategory.RATE_LIMIT);
        assertThat(classifier.classify(http(401, null), null).category()).isEqualTo(ErrorCategory.AUTH);
    }

    // ---- helpers ----

    private static HttpClientErrorException http(int status, String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.add(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return HttpClientErrorException.create(HttpStatusCode.valueOf(status), "err",
                headers, new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpServerErrorException serverHttp(int status) {
        return HttpServerErrorException.create(HttpStatusCode.valueOf(status), "err",
                new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }

    private static ChatClientResponse responseWithFinishReason(String finishReason) {
        ChatGenerationMetadata metadata = ChatGenerationMetadata.builder().finishReason(finishReason).build();
        ChatResponse chat = new ChatResponse(List.of(new Generation(new AssistantMessage(""), metadata)));
        return ChatClientResponse.builder().chatResponse(chat).build();
    }
}
