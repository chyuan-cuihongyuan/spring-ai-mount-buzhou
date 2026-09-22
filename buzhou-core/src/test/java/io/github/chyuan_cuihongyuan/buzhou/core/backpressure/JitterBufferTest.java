package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1903 / T3008：抖动缓冲——覆盖分位、覆盖率、极值、畸形。 */
class JitterBufferTest {

    private static final long[] SAMPLES = {10, 20, 30, 40, 50};

    /** 覆盖分位：target 0.9 → 第 ⌈4.5⌉=5 个 = 50ms；target 0.5 → 30ms。 */
    @Test
    void requiredDelayCoversTarget() {
        assertThat(JitterBuffer.requiredDelay(SAMPLES, 0.9)).isEqualTo(50L);
        assertThat(JitterBuffer.requiredDelay(SAMPLES, 0.5)).isEqualTo(30L);
    }

    /** 覆盖率读数：延迟 30 覆盖 3/5；延迟 50 覆盖全量。 */
    @Test
    void coverageRatioScales() {
        assertThat(JitterBuffer.coverageRatio(SAMPLES, 30)).isCloseTo(0.6, within(1e-12));
        assertThat(JitterBuffer.coverageRatio(SAMPLES, 50)).isCloseTo(1.0, within(1e-12));
        assertThat(JitterBuffer.coverageRatio(SAMPLES, 5)).isCloseTo(0.0, within(1e-12));
    }

    /** target=1.0 极值：取最大样本一个不落。 */
    @Test
    void fullCoverageTakesMax() {
        assertThat(JitterBuffer.requiredDelay(SAMPLES, 1.0)).isEqualTo(50L);
    }

    /** 畸形入参 fail-fast：空表、target 越界、负样本、负延迟。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> JitterBuffer.requiredDelay(new long[0], 0.9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("样本表不能为空");
        assertThatThrownBy(() -> JitterBuffer.requiredDelay(SAMPLES, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetCoverage 须在 (0,1]");
        assertThatThrownBy(() -> JitterBuffer.requiredDelay(new long[]{-1}, 0.9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("到达延迟不能为负");
        assertThatThrownBy(() -> JitterBuffer.coverageRatio(SAMPLES, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delay 不能为负");
    }
}
