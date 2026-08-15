package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 50 §B / T179 / impl-148：webhook outbox 退避抖动域——注入确定性随机源钉边界
 * （base=1s×2^attempts 封顶 60s；抖动后 ∈ [0.75, 1.25]×base）。
 */
class WebhookBackoffJitterTest {

    @Test
    void jitterBoundsHoldAcrossAttempts() {
        for (int attempts = 0; attempts <= 7; attempts++) {
            long base = Math.min(1000L << attempts, 60_000L);
            assertThat(WebhookEventForwarder.jitteredBackoffMillis(attempts, () -> 0.0))
                    .as("随机源 0.0 → 0.75×base（attempts=%d）", attempts)
                    .isEqualTo(Math.round(base * 0.75));
            assertThat(WebhookEventForwarder.jitteredBackoffMillis(attempts, () -> 0.5))
                    .as("随机源 0.5 → base", attempts)
                    .isEqualTo(base);
            assertThat(WebhookEventForwarder.jitteredBackoffMillis(attempts, () -> 1.0))
                    .as("随机源 1.0 → 1.25×base（attempts=%d）", attempts)
                    .isEqualTo(Math.round(base * 1.25));
        }
        // 封顶：高 attempts 不会突破 60s×1.25
        assertThat(WebhookEventForwarder.jitteredBackoffMillis(20, () -> 1.0))
                .isEqualTo(Math.round(60_000L * 1.25));
    }
}
