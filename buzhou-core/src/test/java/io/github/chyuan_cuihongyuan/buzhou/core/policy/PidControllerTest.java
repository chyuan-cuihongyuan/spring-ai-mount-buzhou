package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PidControllerTest {

    /** 一阶惯性植物+恒定负载：y ← y + 0.1·out − 0.02（负载使 P-only 稳态余差显形——经典对照前提）。 */
    private static double plantStep(double y, double out) {
        return y + 0.1 * out - 0.02;
    }

    @Test
    void shouldConvergeWithPiAndEliminateSteadyStateError() {
        // PI 闭环圣像：500 拍后输出趋于 0（稳态误差被积分消掉——植物无扰时）
        PidController pi = new PidController(0.8, 0.15, 0, -5, 5);
        double y = 0;
        for (int i = 0; i < 500; i++) {
            y = plantStep(y, pi.update(1.0, y));
        }
        assertThat(y).isCloseTo(1.0, within(1e-3));
        // P-only 对照：同植物存在稳态余差（最后误差显著非零）
        PidController pOnly = new PidController(0.8, 0, 0, -5, 5);
        double y2 = 0;
        for (int i = 0; i < 500; i++) {
            y2 = plantStep(y2, pOnly.update(1.0, y2));
        }
        // P-only 收敛到 y = 0.8(1−y)/… 稳态 y=Kp/(Kp+10)·? —— 余差由断言呈现（显著 < 1）
        assertThat(y2).isLessThan(1.0);
        assertThat(1.0 - y2).isGreaterThan(0.05);
        assertThat(y).isGreaterThan(y2); // PI 优于 P-only
    }

    @Test
    void shouldDampOscillationWithDerivative() {
        // D 抑振圣像：强 Ki 振荡植物上，加 Kd 的超调显著小于无 D
        double overshootNoD = simulateOvershoot(0.0);
        double overshootWithD = simulateOvershoot(0.35);
        assertThat(overshootWithD).isLessThan(overshootNoD);
    }

    private double simulateOvershoot(double kd) {
        PidController pid = new PidController(1.2, 2.0, kd, -10, 10);
        double y = 0;
        double peak = 0;
        for (int i = 0; i < 800; i++) {
            y = plantStep(y, pid.update(1.0, y));
            peak = Math.max(peak, y);
        }
        return peak - 1.0;
    }

    @Test
    void shouldAntiWindupAndFailFast() {
        // 抗饱和：深度饱和期积分不膨胀（输出夹挤 0..1，误差恒 100）
        PidController pid = new PidController(1, 1, 0, 0, 1);
        for (int i = 0; i < 1000; i++) {
            assertThat(pid.update(100.0, 0)).isEqualTo(1.0); // 饱和输出
        }
        assertThat(pid.integral()).isZero(); // 未入积——抗饱和
        // 饱和解除后快速回落（无 windup 拖尾）
        double out = pid.update(0.5, 0.5);
        assertThat(out).isLessThan(1.0);
        assertThatThrownBy(() -> new PidController(-1, 0, 0, 0, 1))
                .hasMessageContaining("增益非负");
        assertThatThrownBy(() -> new PidController(1, 0, 0, 5, 1))
                .hasMessageContaining("min<max");
        assertThatThrownBy(() -> new PidController(1, 0, 0, 0, 1).update(Double.NaN, 0))
                .hasMessageContaining("NaN");
        // 确定性双跑
        PidController a = new PidController(1, 0.5, 0.1, -2, 2);
        PidController b = new PidController(1, 0.5, 0.1, -2, 2);
        for (int i = 0; i < 100; i++) {
            assertThat(a.update(1, 0.3 * i / 100.0)).isEqualTo(b.update(1, 0.3 * i / 100.0));
        }
    }
}
