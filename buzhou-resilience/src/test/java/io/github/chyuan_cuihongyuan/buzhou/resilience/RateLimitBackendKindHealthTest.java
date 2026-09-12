package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流后端形态健康面测试（spec 637 / T924–T925 / impl 490）：
 * stats.details().rateLimitBackend 随装配形态（memory / memory-gcra）一读便知。
 */
class RateLimitBackendKindHealthTest {

    private static ResilienceStats configureWith(ResilienceProperties.RateLimit rl) {
        ResilienceStats stats = new ResilienceStats();
        ResilienceModule.configure(new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                rl, null, null, null, null, null, null), "m", stats,
                null, null, null, null, null);
        return stats;
    }

    /** 默认令牌桶 → memory；smoothing=gcra → memory-gcra；未配置 → none。 */
    @Test
    void backendKindVisibleInStats() {
        var tokenBucket = configureWith(new ResilienceProperties.RateLimit(
                60, 1000, Duration.ofSeconds(1), null));
        assertThat(tokenBucket.details()).containsEntry("rateLimitBackend", "memory");

        var gcra = configureWith(new ResilienceProperties.RateLimit(
                60, 1000, Duration.ofSeconds(1), null, "gcra", null));
        assertThat(gcra.details()).containsEntry("rateLimitBackend", "memory-gcra");

        ResilienceStats none = new ResilienceStats();
        ResilienceModule.configure(new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                null, null, null, null, null, null, null), "m", none,
                null, null, null, null, null);
        assertThat(none.details()).containsEntry("rateLimitBackend", "none");
    }
}
