package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GradientBanditTest {

    @Test
    void shouldCarrySoftmaxAnchors() {
        // 全零偏好：均匀 softmax
        double[] pi = GradientBandit.probabilities(new double[]{0, 0, 0, 0});
        for (double p : pi) {
            assertThat(p).isCloseTo(0.25, within(1e-12));
        }
        // 偏好差 ln3：π 比恰为 3:1（softmax 不变性锚）
        double[] ratio = GradientBandit.probabilities(new double[]{Math.log(3), 0});
        assertThat(ratio[0] / ratio[1]).isCloseTo(3.0, within(1e-9));
        // 平移不变：整体 +100 同分布（数值稳定锚）
        double[] shifted = GradientBandit.probabilities(new double[]{100 + Math.log(3), 100});
        assertThat(shifted[0]).isCloseTo(ratio[0], within(1e-9));
    }

    @Test
    void shouldLearnPreferencesTowardBetterArm() {
        // 圣像：0 号臂奖励 N(高)、1 号臂 N(低)——偏好分岔、softmax 压向 0
        Random random = new Random(7);
        double[] preferences = {0, 0};
        double alpha = 0.1;
        double baseline = 0;
        for (int round = 0; round < 30000; round++) {
            int arm = GradientBandit.select(preferences, random);
            double reward = arm == 0 ? 1.0 + random.nextGaussian() * 0.1
                                     : 0.0 + random.nextGaussian() * 0.1;
            GradientBandit.update(preferences, arm, reward, baseline, alpha);
            // 滑动均值基线（第 2 轮起）——纯函数面由调用方维护基线
            baseline = baseline * 0.999 + reward * 0.001;
        }
        assertThat(preferences[0] - preferences[1]).isGreaterThan(1.0);
        double best = GradientBandit.probabilities(preferences)[0];
        assertThat(best).isGreaterThan(0.8);
    }

    @Test
    void shouldBeFailFastAndDeterministic() {
        assertThatThrownBy(() -> GradientBandit.probabilities(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GradientBandit.probabilities(new double[]{Double.NaN}))
                .hasMessageContaining("NaN");
        assertThatThrownBy(() -> GradientBandit.update(new double[]{0, 0}, 5, 1, 0, 0.1))
                .hasMessageContaining("臂越域");
        assertThatThrownBy(() -> GradientBandit.update(new double[]{0, 0}, 0, 1, 0, 0))
                .hasMessageContaining("α");
        assertThatThrownBy(() -> GradientBandit.update(new double[]{0, 0}, 0, Double.NaN, 0, 0.1))
                .hasMessageContaining("NaN");
        // 确定性：同种子同选择 + 更新确定性
        assertThat(GradientBandit.select(new double[]{0.5, 0}, new Random(9)))
                .isEqualTo(GradientBandit.select(new double[]{0.5, 0}, new Random(9)));
        double[] first = {0, 0};
        double[] second = {0, 0};
        GradientBandit.update(first, 0, 1.0, 0.5, 0.2);
        GradientBandit.update(second, 0, 1.0, 0.5, 0.2);
        assertThat(first).containsExactly(second);
    }
}
