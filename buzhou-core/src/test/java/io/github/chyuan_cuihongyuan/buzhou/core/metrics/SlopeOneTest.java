package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11022 / Y11045：SlopeOne 合同验证——手算偏离互证+已评项恒等
 * +无基座 fail-fast+确定性。
 */
class SlopeOneTest {

    @Test
    void shouldMatchManualDeviation_whenTinyMatrix() {
        // 手算：预测 u0 对 item2（u0 已评 item0=3、未评 item1）
        // j=0：共评者=同评 item0 与 item2 者 → 仅 u3(5,2)：dev(0,2)=3，freq=1
        // j=1：u0 未评 item1 → 跳过
        // pred=(3+3)·1/1=6
        double[][] ratings = {
                {3, Double.NaN, Double.NaN},
                {2, 1, Double.NaN},
                {4, 3, Double.NaN},
                {5, Double.NaN, 2}};
        double prediction = SlopeOne.predict(ratings, 0, 2);
        assertThat(prediction).isCloseTo(6.0, within(1e-9));
    }

    @Test
    void shouldBeIdentity_whenItemAlreadyRated() {
        double[][] ratings = {
                {1, 2, 3},
                {2, 3, 4}};
        assertThat(SlopeOne.predict(ratings, 1, 2)).isCloseTo(4.0, within(1e-9));
    }

    @Test
    void shouldBeDeterministic_whenSameInputTwice() {
        double[][] ratings = {
                {1, 2, Double.NaN},
                {3, 4, 5},
                {Double.NaN, 1, 2}};
        assertThat(SlopeOne.predict(ratings, 0, 2))
                .isEqualTo(SlopeOne.predict(ratings, 0, 2));
    }

    @Test
    void shouldFailFast_whenNoBasisOrOutOfRange() {
        double[][] ratings = {
                {Double.NaN, Double.NaN},
                {1, 2}};
        // item1 无共评者（u0 未评 item0）
        assertThatThrownBy(() -> SlopeOne.predict(ratings, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无共评基座");
        assertThatThrownBy(() -> SlopeOne.predict(ratings, 5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户越界");
        assertThatThrownBy(() -> SlopeOne.predict(null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
