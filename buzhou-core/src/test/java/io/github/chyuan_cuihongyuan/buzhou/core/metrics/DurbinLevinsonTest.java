package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11010 / Y11021：DurbinLevinson 合同验证——AR(1) 闭式手锚
 * +AR(2) YuleWalker 逆变换往返互证圣像+确定性+fail-fast。
 */
class DurbinLevinsonTest {

    @Test
    void shouldMatchClosedForm_whenArOne() {
        DurbinLevinson.ArModel model = DurbinLevinson.solve(new double[]{0.8});
        assertThat(model.coefficients()[0]).isCloseTo(0.8, within(1e-12));
        assertThat(model.errorVariance()).isCloseTo(1.0 - 0.64, within(1e-12));
    }

    @Test
    void shouldRoundTrip_whenArTwoTheoreticalAcf() {
        // AR(2) a1=0.5, a2=0.3 → 理论 ACF：r1=a1/(1−a2), r2=a1·r1+a2
        double a1 = 0.5;
        double a2 = 0.3;
        double r1 = a1 / (1.0 - a2);
        double r2 = a1 * r1 + a2;
        DurbinLevinson.ArModel model = DurbinLevinson.solve(new double[]{r1, r2});
        assertThat(model.coefficients()[0]).isCloseTo(a1, within(1e-9));
        assertThat(model.coefficients()[1]).isCloseTo(a2, within(1e-9));
        // 归一化 r(0)=1 域：E(p)=σ²=1−a1·r1−a2·r2
        assertThat(model.errorVariance())
                .isCloseTo(1.0 - a1 * r1 - a2 * r2, within(1e-9));
    }

    @Test
    void shouldMonotonicallyDecreaseError_whenOrderGrows() {
        double[] r = {0.9, 0.81, 0.729, 0.6561};
        for (int order = 1; order <= 4; order++) {
            double[] prefix = new double[order];
            System.arraycopy(r, 0, prefix, 0, order);
            DurbinLevinson.ArModel model = DurbinLevinson.solve(prefix);
            assertThat(model.errorVariance()).isGreaterThanOrEqualTo(0.0);
            if (order > 1) {
                DurbinLevinson.ArModel previous = DurbinLevinson.solve(
                        java.util.Arrays.copyOf(prefix, order - 1));
                assertThat(model.errorVariance())
                        .isLessThanOrEqualTo(previous.errorVariance() + 1e-12);
            }
        }
    }

    @Test
    void shouldReproduceIdenticalModel_whenSameInputTwice() {
        double[] r = {0.7, 0.4};
        DurbinLevinson.ArModel first = DurbinLevinson.solve(r);
        DurbinLevinson.ArModel second = DurbinLevinson.solve(r);
        assertThat(second.coefficients()).isEqualTo(first.coefficients());
        assertThat(second.errorVariance()).isEqualTo(first.errorVariance());
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> DurbinLevinson.solve(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurbinLevinson.solve(new double[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非空");
        assertThatThrownBy(() -> DurbinLevinson.solve(new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("|r|<1");
    }
}
