package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1844 / T2890：重连阶梯——指数爬升、封顶、放弃边界、溢出安全。 */
class ReconnectBackoffLadderTest {

    /** 指数爬升：100×2 阶梯 1/2/3/4 次 → 100/200/400/800。 */
    @Test
    void shouldClimbExponentially() {
        assertThat(ReconnectBackoffLadder.delayMillis(1, 100, 2, 10_000))
                .isEqualTo(100L);
        assertThat(ReconnectBackoffLadder.delayMillis(2, 100, 2, 10_000))
                .isEqualTo(200L);
        assertThat(ReconnectBackoffLadder.delayMillis(4, 100, 2, 10_000))
                .isEqualTo(800L);
    }

    /** 封顶钳制：cap 300 下第 3 次（400）即 300；天文 attempt 仍 300（溢出安全）。 */
    @Test
    void capClampsAndOverflowSafe() {
        assertThat(ReconnectBackoffLadder.delayMillis(3, 100, 2, 300)).isEqualTo(300L);
        assertThat(ReconnectBackoffLadder.delayMillis(1000, 100, 2, 300)).isEqualTo(300L);
        assertThat(ReconnectBackoffLadder.delayMillis(Integer.MAX_VALUE, 100, 2, Long.MAX_VALUE / 2))
                .isPositive();
    }

    /** 放弃边界含：第 maxAttempts 次仍 RETRY、超一次即 GIVE_UP。 */
    @Test
    void giveUpBoundaryIsExclusive() {
        assertThat(ReconnectBackoffLadder.verdict(5, 5))
                .isEqualTo(ReconnectBackoffLadder.Verdict.RETRY);
        assertThat(ReconnectBackoffLadder.verdict(6, 5))
                .isEqualTo(ReconnectBackoffLadder.Verdict.GIVE_UP);
    }

    /** 畸形入参 fail-fast：零次尝试、base<1、multiplier<1、cap<base。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ReconnectBackoffLadder.delayMillis(0, 100, 2, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempt 不能小于 1");
        assertThatThrownBy(() -> ReconnectBackoffLadder.delayMillis(1, 0, 2, 300))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReconnectBackoffLadder.delayMillis(1, 100, 1, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cap≥base");
        assertThatThrownBy(() -> ReconnectBackoffLadder.verdict(0, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
