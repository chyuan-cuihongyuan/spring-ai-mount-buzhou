package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1911 / T3024：客户端节流——健康零概率、过载爬升、掷骰、畸形。 */
class ClientThrottleProbabilityTest {

    /** 健康期：100 发 90 收 K=2 → 概率 0 零干扰。 */
    @Test
    void healthyPeriodZeroProbability() {
        assertThat(ClientThrottleProbability.rejectProbability(100, 90, 2.0))
                .isCloseTo(0.0, within(1e-12));
    }

    /** 过载期：100 发 40 收 K=2 → (100−80)/101 ≈ 0.198。 */
    @Test
    void overloadedClimbs() {
        assertThat(ClientThrottleProbability.rejectProbability(100, 40, 2.0))
                .isCloseTo(20.0 / 101, within(1e-12));
    }

    /** 极端积压：200 发 50 收 K=2 → ≈0.4975；掷骰边界两例。 */
    @Test
    void extremeBacklogAndDice() {
        double p = ClientThrottleProbability.rejectProbability(200, 50, 2.0);
        assertThat(p).isCloseTo(100.0 / 201, within(1e-12));
        assertThat(ClientThrottleProbability.shouldDrop(p, p - 1e-9)).isTrue();
        assertThat(ClientThrottleProbability.shouldDrop(p, p + 1e-9)).isFalse();
    }

    /** 畸形入参 fail-fast：负计数、K<1、概率/骰子越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ClientThrottleProbability.rejectProbability(-1, 0, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requests 不能为负");
        assertThatThrownBy(() -> ClientThrottleProbability.rejectProbability(0, -1, 2.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accepts 不能为负");
        assertThatThrownBy(() -> ClientThrottleProbability.rejectProbability(0, 0, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ratioK 不能小于 1");
        assertThatThrownBy(() -> ClientThrottleProbability.shouldDrop(0.5, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dice 须在 [0,1]");
    }
}
