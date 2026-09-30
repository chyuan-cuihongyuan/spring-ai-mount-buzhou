package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FastInverseSqrtTest {

    @Test
    void shouldStayWithinErrorContract() {
        // 误差契约圣像：全域对数刻度采样相对误差 <0.2%
        Random random = new Random(41);
        for (int t = 0; t < 10000; t++) {
            float x = (float) Math.pow(10, random.nextDouble() * 12 - 6); // 1e-6 .. 1e6
            float approx = FastInverseSqrt.inverseSqrt(x);
            double exact = 1.0 / Math.sqrt(x);
            double relative = Math.abs(approx - exact) / exact;
            assertThat(relative).as("x=%g", x).isLessThan(0.002);
        }
        // 锚值：4→≈0.5、9→≈1/3（肉眼级）
        assertThat(FastInverseSqrt.inverseSqrt(4f)).isCloseTo(0.5f, within(0.001f));
        assertThat(FastInverseSqrt.inverseSqrt(9f)).isCloseTo(0.33333f, within(0.001f));
        assertThat(FastInverseSqrt.inverseSqrt(1e10f)).isCloseTo(1e-5f, within(2e-8f)); // 0.2% 契约内
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        assertThat(FastInverseSqrt.inverseSqrt(2f)).isEqualTo(FastInverseSqrt.inverseSqrt(2f));
        assertThatThrownBy(() -> FastInverseSqrt.inverseSqrt(0f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FastInverseSqrt.inverseSqrt(-4f))
                .hasMessageContaining("有限正数");
        assertThatThrownBy(() -> FastInverseSqrt.inverseSqrt(Float.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FastInverseSqrt.inverseSqrt(Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
