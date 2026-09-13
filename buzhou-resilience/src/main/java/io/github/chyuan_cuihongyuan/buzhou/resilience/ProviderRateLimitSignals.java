package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * 供应商限流头前瞻读数（spec 719 / T1038，OpenAI x-ratelimit-* 思想）：
 * 从响应头解析余量/上限/reset——429 之前的拥挤信号。DefaultErrorClassifier
 * 是事后（Retry-After），本面是事前。
 *
 * <p>纯解析原语：advisor 拦截与自动降速归消费端。单响应快照口径；
 * 全字段 null-safe、畸形值跳过（fail-safe 不抛）；无任何相关头 → empty()。
 * 头名按 OpenAI 通行约定（不同供应商头名异——宿主自行归一后调用）。
 */
public final class ProviderRateLimitSignals {

    private ProviderRateLimitSignals() {
    }

    /** 压力分级（utilization 阈值：MEDIUM ≥0.8 / HIGH ≥0.95；取请求/Token 两者较大值）。 */
    public enum Pressure { NONE, MEDIUM, HIGH }

    /** 不可变信号集（各字段可空——供应商可能只带部分头）。 */
    public record Signals(Long remainingRequests, Long limitRequests,
                          Long remainingTokens, Long limitTokens,
                          Duration resetRequests, Duration resetTokens) {

        public static final Signals EMPTY =
                new Signals(null, null, null, null, null, null);

        /** 请求余量利用率：1 − remaining/limit（缺 limit 或 remaining → NaN）。 */
        public double requestUtilization() {
            return utilization(remainingRequests, limitRequests);
        }

        /** Token 余量利用率（口径同上）。 */
        public double tokenUtilization() {
            return utilization(remainingTokens, limitTokens);
        }

        /** 压力分级（两者较大值——任一维度拥挤即拥挤）。 */
        public Pressure pressureLevel() {
            double worst = Math.max(nonNan(requestUtilization()), nonNan(tokenUtilization()));
            if (worst >= 0.95) {
                return Pressure.HIGH;
            }
            return worst >= 0.8 ? Pressure.MEDIUM : Pressure.NONE;
        }

        private static double utilization(Long remaining, Long limit) {
            if (remaining == null || limit == null || limit <= 0) {
                return Double.NaN;
            }
            return 1.0 - ((double) remaining / limit);
        }

        private static double nonNan(double value) {
            return Double.isNaN(value) ? 0.0 : value;
        }
    }

    /** 解析（null headers fail-fast；无相关头 = {@link Signals#EMPTY}）——OpenAI 头名约定。 */
    public static Signals parse(HttpHeaders headers) {
        Objects.requireNonNull(headers, "headers");
        return new Signals(
                longHeader(headers, "X-RateLimit-Remaining-Requests"),
                longHeader(headers, "X-RateLimit-Limit-Requests"),
                longHeader(headers, "X-RateLimit-Remaining-Tokens"),
                longHeader(headers, "X-RateLimit-Limit-Tokens"),
                durationHeader(headers, "X-RateLimit-Reset-Requests"),
                durationHeader(headers, "X-RateLimit-Reset-Tokens"));
    }

    /**
     * spec 730 / T1054 族扩散：跨供应商归一解析——先 OpenAI 头名，缺项再回退
     * Anthropic 头名（anthropic-ratelimit-requests-remaining/tokens-limit 等）。
     * 两家头都在时 OpenAI 优先（不混合来源）。
     */
    public static Signals parseFlexible(HttpHeaders headers) {
        Signals openai = parse(headers);
        if (!openai.equals(Signals.EMPTY)) {
            return openai; // 有任一 OpenAI 头——不混合来源
        }
        return new Signals(
                longHeader(headers, "Anthropic-RateLimit-Requests-Remaining"),
                longHeader(headers, "Anthropic-RateLimit-Requests-Limit"),
                longHeader(headers, "Anthropic-RateLimit-Tokens-Remaining"),
                longHeader(headers, "Anthropic-RateLimit-Tokens-Limit"),
                durationHeader(headers, "Anthropic-RateLimit-Tokens-Reset"),
                durationHeader(headers, "Anthropic-RateLimit-Tokens-Reset"));
    }

    private static Long longHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null; // 畸形 fail-safe
        }
    }

    /**
     * reset 头解析：delta-seconds / 复合时长（1m20s）/ HTTP-date。
     * 解析失败 → null（Date 口径需当前时刻，返回距该时刻的负值不合理——保守跳过）。
     */
    private static Duration durationHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("s") || trimmed.endsWith("m") || trimmed.endsWith("h")) {
            return parseCompound(trimmed.toLowerCase(Locale.ROOT));
        }
        try {
            long epochSecond = Long.parseLong(trimmed);
            return Duration.ofSeconds(epochSecond); // 纯数字 = delta-seconds
        } catch (NumberFormatException ignored) {
            // 尝试 HTTP-date
        }
        try {
            ZonedDateTime date = ZonedDateTime.parse(trimmed,
                    DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC));
            long seconds = date.toEpochSecond() - System.currentTimeMillis() / 1000;
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 复合时长（如 1h2m30s；各段可缺省）。 */
    private static Duration parseCompound(String value) {
        long seconds = 0;
        long current = -1;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                current = current < 0 ? 0 : current;
                current = current * 10 + (ch - '0');
            } else {
                if (current < 0) {
                    return null; // 非法序列
                }
                seconds += switch (ch) {
                    case 's' -> current;
                    case 'm' -> current * 60;
                    case 'h' -> current * 3600;
                    default -> -1;
                };
                if (seconds < 0) {
                    return null;
                }
                current = -1;
            }
        }
        return current < 0 ? Duration.ofSeconds(seconds) : null;
    }
}
