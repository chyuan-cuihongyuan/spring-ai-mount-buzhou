package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 745 / T1092–T1093：响应缓存权重预算 yml 装配——record 绑定+兼容构造+
 * 负值 fail-fast。
 */
class ResponseCacheWeightAssemblyTest {

    @Test
    void recordBindingAndCompatConstructor() {
        ResilienceProperties.ResponseCache full = new ResilienceProperties.ResponseCache(
                Boolean.TRUE, 128, Duration.ofHours(1), 50_000L);
        assertThat(full.maxWeightChars()).isEqualTo(50_000L);

        ResilienceProperties.ResponseCache legacy = new ResilienceProperties.ResponseCache(
                Boolean.TRUE, 128, Duration.ofHours(1));
        assertThat(legacy.maxWeightChars()).isZero(); // 兼容构造默认关

        assertThatThrownBy(() -> new ResilienceProperties.ResponseCache(
                Boolean.TRUE, 128, Duration.ofHours(1), -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
