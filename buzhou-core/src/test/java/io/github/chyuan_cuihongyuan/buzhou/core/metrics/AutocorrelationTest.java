package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3037 / T5076：ACF 合同——交替列 lag1=−1/lag2=+1 恰值、趋势
 * 惯性高正、白噪各 lag ~0、正弦周期自锁定 ~1、常数列 NaN、参数
 * fail-fast。
 */
class AutocorrelationTest {

    @Test
    void alternatingSeriesShouldBePerfectlyAnticorrelated() {
        double[] alternating = new double[100];
        for (int i = 0; i < alternating.length; i++) {
            alternating[i] = i % 2 == 0 ? 1 : -1;
        }
        assertThat(Autocorrelation.lagK(alternating, 1)).isCloseTo(-1, within(1e-9));
        assertThat(Autocorrelation.lagK(alternating, 2)).isCloseTo(1, within(1e-9));
    }

    @Test
    void trendingSeriesShouldShowInertia() {
        double[] ramp = new double[100];
        for (int i = 0; i < ramp.length; i++) {
            ramp[i] = i + 1;
        }
        assertThat(Autocorrelation.lagK(ramp, 1)).isGreaterThan(0.95);
        assertThat(Autocorrelation.lagK(ramp, 10)).isGreaterThan(0.6);
    }

    @Test
    void whiteNoiseShouldStayNearZero() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(3);
        double[] noise = new double[500];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = rng.nextGaussian();
        }
        for (int lag = 1; lag <= 5; lag++) {
            assertThat(Autocorrelation.lagK(noise, lag))
                    .as("lag %d", lag)
                    .isLessThan(0.15)
                    .isGreaterThan(-0.15);
        }
    }

    @Test
    void periodicSeriesShouldSelfLockAtPeriod() {
        double[] sine = new double[200];
        for (int i = 0; i < sine.length; i++) {
            sine[i] = Math.sin(2 * Math.PI * i / 10);   // 周期 10
        }
        assertThat(Autocorrelation.lagK(sine, 10)).isCloseTo(1, within(1e-9));
        assertThat(Autocorrelation.lagK(sine, 5)).isCloseTo(-1, within(1e-9));  // 半周期反相
    }

    @Test
    void constantSeriesShouldBeNaN() {
        double[] flat = new double[50];
        java.util.Arrays.fill(flat, 7.0);
        assertThat(Autocorrelation.lagK(flat, 1)).isNaN();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> Autocorrelation.lagK(new double[] {1}, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Autocorrelation.lagK(new double[10], 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Autocorrelation.lagK(new double[10], 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Autocorrelation.lagK(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
