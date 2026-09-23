package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 4037 / T6076：Holt-Winters 季节合同——季节信号逐位预测、
 * 常值收敛、确定性回放、参数/观测 fail-fast、预热未毕业 ISE。
 */
class HoltWintersIndexTest {

    private static final int PERIOD = 4;
    private static final double ALPHA = 0.5;
    private static final double BETA = 0.3;
    private static final double GAMMA = 0.4;
    private static final double BASE = 10.0;
    private static final double SLOPE = 2.0;

    /** 线性 + 周期季节的真值函数。 */
    private static double series(int position) {
        double[] season = {0.0, 5.0, -5.0, 0.0};
        return BASE + SLOPE * position + season[position % PERIOD];
    }

    @Test
    void seasonalSignalShouldBeTrackedToWithinTolerance() {
        HoltWintersIndex model = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        for (int position = 0; position < 5 * PERIOD; position++) {
            model.observe(series(position));
        }
        assertThat(model.warmedUp()).isTrue();
        for (int horizon = 1; horizon <= PERIOD; horizon++) {
            double expected = series(5 * PERIOD + horizon - 1);
            assertThat(model.forecast(horizon))
                    .as("预测 +%s 步", horizon)
                    .isCloseTo(expected, within(2.0));
        }
        assertThat(model.trend()).isCloseTo(SLOPE, within(0.5));
    }

    @Test
    void constantSeriesShouldCollapseSeasonality() {
        HoltWintersIndex model = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        for (int i = 0; i < 6 * PERIOD; i++) {
            model.observe(100.0);
        }
        for (int slot = 0; slot < PERIOD; slot++) {
            assertThat(model.seasonalIndexOf(slot)).isCloseTo(0.0, within(0.01));
        }
        assertThat(model.level()).isCloseTo(100.0, within(0.5));
        assertThat(model.forecast(1)).isCloseTo(100.0, within(0.5));
    }

    @Test
    void sameObservationsShouldReplaySameForecast() {
        HoltWintersIndex first = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        HoltWintersIndex second = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        for (int position = 0; position < 4 * PERIOD; position++) {
            first.observe(series(position));
            second.observe(series(position));
        }
        assertThat(first.forecast(3)).isEqualTo(second.forecast(3));
        assertThat(first.seasonalIndexOf(2)).isEqualTo(second.seasonalIndexOf(2));
    }

    @Test
    void invalidParametersAndObservationsShouldFailFast() {
        assertThatThrownBy(() -> new HoltWintersIndex(1, ALPHA, BETA, GAMMA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HoltWintersIndex(PERIOD, 0, BETA, GAMMA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HoltWintersIndex(PERIOD, ALPHA, BETA, 1))
                .isInstanceOf(IllegalArgumentException.class);
        HoltWintersIndex model = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        assertThatThrownBy(() -> model.observe(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unwarmedModelShouldRefuseToGuess() {
        HoltWintersIndex model = new HoltWintersIndex(PERIOD, ALPHA, BETA, GAMMA);
        model.observe(1);
        assertThat(model.warmedUp()).isFalse();
        assertThatThrownBy(model::level).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> model.forecast(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> model.seasonalIndexOf(0)).isInstanceOf(IllegalStateException.class);
    }
}
