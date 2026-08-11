package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 框架内置默认分类器（spec「归一化错误分类」）。
 *
 * <p>五类归一化（provider 无关，不引入 provider SDK 依赖）：
 * <ul>
 *   <li><b>限流 RATE_LIMIT</b>：HTTP 429，解析 Retry-After 头。</li>
 *   <li><b>鉴权 AUTH</b>：HTTP 401 / 403。</li>
 *   <li><b>网络 NETWORK</b>：HTTP 5xx（服务端瞬时故障，按 spec 故事 6 重试——5 类别无独立「服务端」桶，
 *       归入 NETWORK 作为可重试的瞬时基础设施类），以及连接重置 / 读超时 / 瞬断（异常类名启发式）。</li>
 *   <li><b>内容 CONTENT</b>：响应元数据 {@code finishReason} 含 {@code content_filter}（静默拒绝，不抛异常）。</li>
 *   <li><b>未知 UNKNOWN</b>：其余（含非 401/403/429 的 4xx）；保守默认不重试。</li>
 * </ul>
 *
 * <p>识别 HTTP 状态走 {@link RestClientResponseException}（Spring AI 的 RestClient 系 provider——OpenAI /
 * Anthropic / Ollama 等默认传输——抛此族异常），覆盖 RestClient 系；非标 provider 经
 * {@link ProviderErrorClassifier} SPI 覆盖。Retry-After 支持秒数与 HTTP-date 两种格式。
 */
public class DefaultErrorClassifier implements ProviderErrorClassifier {

    private static final String CONTENT_FILTER = "content_filter";

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private static final int HTTP_SERVER_ERROR_END = 600;

    @Override
    public Classification classify(Throwable error, ChatClientResponse response) {
        // 内容拒绝（静默通道）：无异常、仅响应元数据。
        if (error == null) {
            if (response != null && isContentRefusal(response)) {
                return Classification.of(ErrorCategory.CONTENT);
            }
            return Classification.of(ErrorCategory.UNKNOWN);
        }
        // 沿因果链查找带 HTTP 状态的异常（RestClient 系 provider）。
        RestClientResponseException http = findHttp(error);
        if (http != null) {
            int status = http.getStatusCode().value();
            if (status == HTTP_TOO_MANY_REQUESTS) {
                return new Classification(ErrorCategory.RATE_LIMIT, parseRetryAfter(http.getResponseHeaders()));
            }
            if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
                return Classification.of(ErrorCategory.AUTH);
            }
            if (status >= HTTP_SERVER_ERROR_START && status < HTTP_SERVER_ERROR_END) {
                return Classification.of(ErrorCategory.NETWORK);
            }
            // 其余 4xx（如 404 / 422）归未知、不重试。
            return Classification.of(ErrorCategory.UNKNOWN);
        }
        // 无 HTTP 状态：按异常类名启发式判网络瞬断。
        if (isNetwork(error)) {
            return Classification.of(ErrorCategory.NETWORK);
        }
        return Classification.of(ErrorCategory.UNKNOWN);
    }

    private boolean isContentRefusal(ChatClientResponse response) {
        try {
            ChatResponse chat = response.chatResponse();
            if (chat == null) {
                return false;
            }
            Generation gen = chat.getResult();
            if (gen != null && gen.getMetadata() != null && gen.getMetadata().getFinishReason() != null) {
                return gen.getMetadata().getFinishReason().toLowerCase().contains(CONTENT_FILTER);
            }
        } catch (Exception ignored) {
            // 元数据缺失不视为内容拒绝。
        }
        return false;
    }

    private RestClientResponseException findHttp(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof RestClientResponseException rce) {
                return rce;
            }
        }
        return null;
    }

    private boolean isNetwork(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            String name = c.getClass().getName();
            if (name.endsWith("IOException")
                    || name.endsWith("ResourceAccessException")
                    || name.contains("Transient")
                    || name.contains("Connection")
                    || name.contains("Socket")
                    || name.contains("Timeout")) {
                return true;
            }
        }
        return false;
    }

    /** Retry-After 头解析：delta-seconds 或 HTTP-date；无法解析返回 null。 */
    private Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException notSeconds) {
            try {
                long now = java.time.Instant.now().getEpochSecond();
                long target = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toEpochSecond();
                long delta = target - now;
                return delta > 0 ? Duration.ofSeconds(delta) : null;
            } catch (Exception notDate) {
                return null;
            }
        }
    }
}
