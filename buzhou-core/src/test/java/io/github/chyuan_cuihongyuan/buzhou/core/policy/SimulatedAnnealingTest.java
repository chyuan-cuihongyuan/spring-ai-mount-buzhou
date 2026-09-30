package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SimulatedAnnealingTest {

    @Test
    void shouldEscapeLocalMinimaOfRastriginLite() {
        // Rastrigin-lite 圣像：E(x)=x²+10(1−cos(2πx)) 多凹坑——退火能跨坑逼近全局 0
        double[] best = SimulatedAnnealing.minimize(
                x -> x[0] * x[0] + 10 * (1 - Math.cos(2 * Math.PI * x[0])),
                new double[]{7.5}, 0.6, 8.0, 0.01, 60000, new Random(13));
        assertThat(best[0]).isCloseTo(0.0, within(0.1));
        // 纯贪心对照会停在起点附近凹坑（7.5 最近整点坑 8 或 7）
        double startEnergy = 7.5 * 7.5 + 10 * (1 - Math.cos(2 * Math.PI * 7.5));
        double bestEnergy = best[0] * best[0] + 10 * (1 - Math.cos(2 * Math.PI * best[0]));
        assertThat(bestEnergy).isLessThan(startEnergy / 100);
    }

    @Test
    void shouldOptimizeMultivariateQuadratic() {
        // 多维二次（碗面）圣像：最优 0 向量，各分量逼近
        double[] best = SimulatedAnnealing.minimize(
                x -> x[0] * x[0] + 4 * x[1] * x[1],
                new double[]{5, -6}, 0.8, 6.0, 0.005, 80000, new Random(29));
        assertThat(best[0]).isCloseTo(0.0, within(0.15));
        assertThat(best[1]).isCloseTo(0.0, within(0.15));
        // best-ever 恒不劣于起点
        double startEnergy = 25 + 4 * 36;
        double bestEnergy = best[0] * best[0] + 4 * best[1] * best[1];
        assertThat(bestEnergy).isLessThan(startEnergy);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        java.util.function.ToDoubleFunction<double[]> energy = x -> x[0] * x[0];
        double[] first = SimulatedAnnealing.minimize(energy, new double[]{3}, 0.5, 4, 0.01, 5000, new Random(77));
        double[] second = SimulatedAnnealing.minimize(energy, new double[]{3}, 0.5, 4, 0.01, 5000, new Random(77));
        assertThat(first).containsExactly(second);
        assertThatThrownBy(() -> SimulatedAnnealing.minimize(null, new double[]{1}, 0.5, 4, 0.01, 100, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimulatedAnnealing.minimize(energy, new double[]{1}, 0, 4, 0.01, 100, new Random()))
                .hasMessageContaining("步长");
        assertThatThrownBy(() -> SimulatedAnnealing.minimize(energy, new double[]{1}, 0.5, 0.01, 4, 100, new Random()))
                .hasMessageContaining("温度");
        assertThatThrownBy(() -> SimulatedAnnealing.minimize(energy, new double[]{1}, 0.5, 4, 0.01, 0, new Random()))
                .hasMessageContaining("迭代");
    }
}
