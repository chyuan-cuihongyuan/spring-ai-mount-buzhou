package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 719 / T1038–T1039：供应商限流头前瞻——解析/利用率/压力分级/
 * fail-safe/empty。
 */
class ProviderRateLimitSignalsTest {

    private static HttpHeaders headers(String... kv) {
        HttpHeaders headers = new HttpHeaders();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            headers.add(kv[i], kv[i + 1]);
        }
        return headers;
    }

    @Test
    void fullHeaderSetParsesAndComputesUtilization() {
        ProviderRateLimitSignals.Signals signals = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "10",
                "X-RateLimit-Remaining-Requests", "3",
                "X-RateLimit-Limit-Tokens", "1000",
                "X-RateLimit-Remaining-Tokens", "700",
                "X-RateLimit-Reset-Requests", "1m20s",
                "X-RateLimit-Reset-Tokens", "2s"));
        assertThat(signals.remainingRequests()).isEqualTo(3);
        assertThat(signals.limitRequests()).isEqualTo(10);
        assertThat(signals.requestUtilization()).isEqualTo(0.7);
        assertThat(signals.tokenUtilization()).isEqualTo(1.0 - 700.0 / 1000.0, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(signals.resetRequests()).isEqualTo(Duration.ofSeconds(80));
        assertThat(signals.resetTokens()).isEqualTo(Duration.ofSeconds(2));
        assertThat(signals.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.NONE);
    }

    @Test
    void pressureLevelsAtThresholds() {
        ProviderRateLimitSignals.Signals medium = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "100", "X-RateLimit-Remaining-Requests", "20"));
        assertThat(medium.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.MEDIUM); // 0.8 边界

        ProviderRateLimitSignals.Signals high = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "100", "X-RateLimit-Remaining-Requests", "4"));
        assertThat(high.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.HIGH); // 0.96

        ProviderRateLimitSignals.Signals below = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "100", "X-RateLimit-Remaining-Requests", "21"));
        assertThat(below.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.NONE); // 0.79

        // token 维度更挤时取较大值
        ProviderRateLimitSignals.Signals mixed = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "100", "X-RateLimit-Remaining-Requests", "99",
                "X-RateLimit-Limit-Tokens", "100", "X-RateLimit-Remaining-Tokens", "3"));
        assertThat(mixed.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.HIGH);
    }

    @Test
    void missingMalformedAndEmptyAreFailSafe() {
        // 缺 limit → NaN
        ProviderRateLimitSignals.Signals partial = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Remaining-Requests", "3"));
        assertThat(partial.requestUtilization()).isNaN();
        assertThat(partial.limitRequests()).isNull();

        // 完全无头 → empty
        assertThat(ProviderRateLimitSignals.parse(headers()).remainingTokens()).isNull();
        ProviderRateLimitSignals.Signals none = ProviderRateLimitSignals.parse(headers("Accept", "*/*"));
        assertThat(none.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.NONE);
        assertThat(none.requestUtilization()).isNaN();

        // 畸形值 fail-safe 跳过
        ProviderRateLimitSignals.Signals malformed = ProviderRateLimitSignals.parse(headers(
                "X-RateLimit-Limit-Requests", "many",
                "X-RateLimit-Reset-Tokens", "soon-ish"));
        assertThat(malformed.limitRequests()).isNull();
        assertThat(malformed.resetTokens()).isNull();

        assertThatThrownBy(() -> ProviderRateLimitSignals.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flexibleParseFallsBackToAnthropicHeaderNames() {
        HttpHeaders anthropic = headers(
                "anthropic-ratelimit-requests-limit", "50",
                "anthropic-ratelimit-requests-remaining", "10",
                "anthropic-ratelimit-tokens-limit", "20000",
                "anthropic-ratelimit-tokens-remaining", "4000");
        ProviderRateLimitSignals.Signals signals = ProviderRateLimitSignals.parseFlexible(anthropic);
        assertThat(signals.limitRequests()).isEqualTo(50);
        assertThat(signals.remainingRequests()).isEqualTo(10);
        assertThat(signals.requestUtilization()).isEqualTo(0.8);
        assertThat(signals.pressureLevel()).isEqualTo(ProviderRateLimitSignals.Pressure.MEDIUM);

        // OpenAI 头优先（两家都在时不混合来源）
        HttpHeaders both = headers(
                "X-RateLimit-Limit-Requests", "10",
                "X-RateLimit-Remaining-Requests", "9",
                "anthropic-ratelimit-requests-limit", "50");
        ProviderRateLimitSignals.Signals openaiFirst = ProviderRateLimitSignals.parseFlexible(both);
        assertThat(openaiFirst.limitRequests()).isEqualTo(10);
        assertThat(openaiFirst.remainingRequests()).isEqualTo(9);
    }
}
