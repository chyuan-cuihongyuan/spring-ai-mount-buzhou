package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11012 / Y11025：YuleWalker 合同验证——AR(1) 闭式手锚+AR(2) 往返
 * 互证圣像+σ² 域换算面+确定性+fail-fast。
 */
class YuleWalkerTest {

    @Test
    void shouldMatchClosedForm_whenArOne() {
        // AR(1) a=0.8、σ²=1：r0=σ²/(1−a²)=2.7778、r1=a·r0
        double a = 0.8;
        double sigma2 = 1.0;
        double r0 = sigma2 / (1.0 - a * a);
        DurbinLevinson.ArModel model = YuleWalker.solve(new double[]{r0, a * r0});
        assertThat(model.coefficients()[0]).isCloseTo(a, within(1e-9));
        assertThat(model.errorVariance()).isCloseTo(sigma2, within(1e-9));
    }

    @Test
    void shouldMatchClosedForm_whenArOneHandScale() {
        // r=[2, 1.6]：归一化 r1=0.8 → a=0.8；σ²=2−0.8·1.6=0.72
        DurbinLevinson.ArModel model = YuleWalker.solve(new double[]{2.0, 1.6});
        assertThat(model.coefficients()[0]).isCloseTo(0.8, within(1e-12));
        assertThat(model.errorVariance()).isCloseTo(0.72, within(1e-12));
    }

    @Test
    void shouldRoundTrip_whenArTwoTheoreticalCovariance() {
        // AR(2) a1=0.5, a2=0.3, σ²=1 → r0=σ²/(1−a1ρ1−a2ρ2)、r_k=a1r_{k−1}+a2r_{k−2}
        double a1 = 0.5;
        double a2 = 0.3;
        double r1 = a1 / (1.0 - a2);
        double r2 = a1 * r1 + a2;
        double r0 = 1.0 / (1.0 - a1 * r1 - a2 * r2);
        // 自协方差域：[r0, r0·ρ1, r0·ρ2]（ρ 与 r0 不可混拼——域一致性勘误入档）
        DurbinLevinson.ArModel model = YuleWalker.solve(
                new double[]{r0, r0 * r1, r0 * r2});
        assertThat(model.coefficients()[0]).isCloseTo(a1, within(1e-9));
        assertThat(model.coefficients()[1]).isCloseTo(a2, within(1e-9));
        assertThat(model.errorVariance()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void shouldReproduceIdenticalModel_whenSameInputTwice() {
        double[] r = {2.0, 1.6, 1.28};
        DurbinLevinson.ArModel first = YuleWalker.solve(r);
        DurbinLevinson.ArModel second = YuleWalker.solve(r);
        assertThat(second.coefficients()).isEqualTo(first.coefficients());
        assertThat(second.errorVariance()).isEqualTo(first.errorVariance());
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> YuleWalker.solve(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> YuleWalker.solve(new double[]{2.0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少一阶");
        assertThatThrownBy(() -> YuleWalker.solve(new double[]{0.0, 0.0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("r0 为正");
    }
}
