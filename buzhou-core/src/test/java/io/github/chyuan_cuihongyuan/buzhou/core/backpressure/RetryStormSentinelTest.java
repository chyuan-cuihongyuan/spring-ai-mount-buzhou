package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1917 / T3036：重试风暴哨兵——占比、判定、畸形。 */
class RetryStormSentinelTest {

    /** 占比读数：100 请求 30 重试 = 0.3；60 重试 = 0.6。 */
    @Test
    void ratioReadout() {
        assertThat(RetryStormSentinel.retryRatio(100, 30)).isCloseTo(0.3, within(1e-12));
        assertThat(RetryStormSentinel.retryRatio(100, 60)).isCloseTo(0.6, within(1e-12));
    }

    /** 风暴判定（阈值 0.5）：0.3 非、0.6 是、恰 0.5 含上是。 */
    @Test
    void stormBoundaryInclusive() {
        assertThat(RetryStormSentinel.isStorm(0.3, 0.5)).isFalse();
        assertThat(RetryStormSentinel.isStorm(0.6, 0.5)).isTrue();
        assertThat(RetryStormSentinel.isStorm(0.5, 0.5)).isTrue();
    }

    /** 畸形入参 fail-fast：零总量、重试超总量、阈值越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RetryStormSentinel.retryRatio(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalRequests 不能小于 1");
        assertThatThrownBy(() -> RetryStormSentinel.retryRatio(100, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retriedRequests 须在 [0, 100]");
        assertThatThrownBy(() -> RetryStormSentinel.isStorm(0.5, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 须在 (0,1]");
    }
}
