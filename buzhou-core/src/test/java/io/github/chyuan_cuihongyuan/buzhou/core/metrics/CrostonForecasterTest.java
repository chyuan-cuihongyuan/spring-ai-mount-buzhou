package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3034 / T5070：Croston 合同——常数周期尺寸收敛（5/3）、全零
 * NaN、首非零初始化、率等价性（毛刺 4@2 期与 8@4 期同率 ~2——
 * 「率才是口径」主张）、双分量手算（α=0.5 二非零 9/1.5=6.0）、
 * 计数对账、参数校验。
 */
class CrostonForecasterTest {

    @Test
    void constantPatternShouldConvergeToSizeOverInterval() {
        // 周期 3 尺寸 5：率收敛 5/3 ≈ 1.667
        CrostonForecaster croston = new CrostonForecaster(0.3);
        for (int cycle = 0; cycle < 60; cycle++) {
            croston.observe(5);
            croston.observe(0);
            croston.observe(0);
        }
        assertThat(croston.forecastPerPeriod()).isCloseTo(5.0 / 3.0, within(0.1));
        assertThat(croston.nonzeroCount()).isEqualTo(60);
    }

    @Test
    void allZerosShouldStayNaN() {
        CrostonForecaster croston = new CrostonForecaster(0.3);
        for (int i = 0; i < 100; i++) {
            croston.observe(0);
        }
        assertThat(croston.forecastPerPeriod()).isNaN();
        assertThat(croston.observations()).isEqualTo(100);
        assertThat(croston.nonzeroCount()).isZero();
    }

    @Test
    void firstNonzeroShouldInitializeDirectly() {
        CrostonForecaster croston = new CrostonForecaster(0.5);
        croston.observe(10);   // 首观测即非零：z'=10、p'=1
        assertThat(croston.demandSize()).isEqualTo(10);
        assertThat(croston.demandInterval()).isEqualTo(1);
        assertThat(croston.forecastPerPeriod()).isEqualTo(10.0);
    }

    @Test
    void rateShouldBeInvariantToBurstShape() {
        // A：每 2 期 4 单；B：每 4 期 8 单——率同为 2（毛刺形状不影响率）
        CrostonForecaster a = new CrostonForecaster(0.3);
        CrostonForecaster b = new CrostonForecaster(0.3);
        for (int i = 0; i < 100; i++) {
            a.observe(4);
            a.observe(0);
            b.observe(i % 4 == 0 ? 8 : 0);
        }
        assertThat(a.forecastPerPeriod()).isCloseTo(2.0, within(0.4));
        assertThat(b.forecastPerPeriod()).isCloseTo(2.0, within(0.4));
        assertThat(a.forecastPerPeriod()).isCloseTo(b.forecastPerPeriod(), within(0.5));
    }

    @Test
    void smoothedComponentsShouldMatchHandComputation() {
        // α=0.5：10（z'=10,p'=1）→ 0 → 8（间隔 2：z'=9、p'=1.5）
        CrostonForecaster croston = new CrostonForecaster(0.5);
        croston.observe(10);
        croston.observe(0);
        croston.observe(8);
        assertThat(croston.demandSize()).isEqualTo(9.0);
        assertThat(croston.demandInterval()).isEqualTo(1.5);
        assertThat(croston.forecastPerPeriod()).isEqualTo(6.0);
    }

    @Test
    void countersShouldAccount() {
        CrostonForecaster croston = new CrostonForecaster(0.3);
        croston.observe(1);
        croston.observe(0);
        croston.observe(0);
        croston.observe(2);
        assertThat(croston.observations()).isEqualTo(4);
        assertThat(croston.nonzeroCount()).isEqualTo(2);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new CrostonForecaster(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrostonForecaster(1)).isInstanceOf(IllegalArgumentException.class);
        CrostonForecaster croston = new CrostonForecaster(0.5);
        assertThatThrownBy(() -> croston.observe(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
