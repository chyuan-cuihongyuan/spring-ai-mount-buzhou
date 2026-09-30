package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class Exp3BanditTest {

    @Test
    void shouldCarryUniformMixtureAnchor() {
        double[] weights = {1, 1, 1, 1};
        for (int arm = 0; arm < 4; arm++) {
            // γ=0.2：p = 0.8·0.25 + 0.2/4 = 0.25
            assertThat(Exp3Bandit.probabilityOf(weights, 0.2, arm)).isCloseTo(0.25, within(1e-12));
        }
        // 全 γ：纯均匀
        assertThat(Exp3Bandit.probabilityOf(weights, 1.0, 0)).isCloseTo(0.25, within(1e-12));
        // 偏置权重：γ=0.1, w={9,1} → p0 = 0.9·0.9 + 0.05 = 0.86
        assertThat(Exp3Bandit.probabilityOf(new double[]{9, 1}, 0.1, 0)).isCloseTo(0.86, within(1e-12));
    }

    @Test
    void shouldLearnBestArmAgainstAdversarialRewards() {
        // 圣像：固定奖励对手（0 号臂恒 1、其余恒 0）——EXP3 选中率应显著压向 0 号臂
        Random random = new Random(42);
        double[] weights = {1, 1, 1};
        double gamma = 0.1;
        int bestPicks = 0;
        for (int round = 0; round < 20000; round++) {
            int arm = Exp3Bandit.select(weights, gamma, random);
            Exp3Bandit.update(weights, arm, arm == 0 ? 1.0 : 0.0, gamma);
            if (arm == 0) {
                bestPicks++;
            }
        }
        assertThat(bestPicks / 20000.0).isGreaterThan(0.6);
        assertThat(weights[0]).isGreaterThan(weights[1]);
        // 确定性：同种子同轨迹
        Random replay = new Random(42);
        double[] replayWeights = {1, 1, 1};
        int firstA = Exp3Bandit.select(replayWeights, gamma, replay);
        int firstB = Exp3Bandit.select(new double[]{1, 1, 1}, gamma, new Random(42));
        assertThat(firstA).isEqualTo(firstB);
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> Exp3Bandit.select(null, 0.1, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Exp3Bandit.select(new double[]{}, 0.1, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Exp3Bandit.select(new double[]{1}, 0, new Random()))
                .hasMessageContaining("γ");
        assertThatThrownBy(() -> Exp3Bandit.select(new double[]{1}, 1.5, new Random()))
                .hasMessageContaining("γ");
        assertThatThrownBy(() -> Exp3Bandit.select(new double[]{-1}, 0.1, new Random()))
                .hasMessageContaining("权重为正");
        assertThatThrownBy(() -> Exp3Bandit.update(new double[]{1, 1}, 0, 1.5, 0.1))
                .hasMessageContaining("奖励域");
        assertThatThrownBy(() -> Exp3Bandit.update(new double[]{1, 1}, 2, 0.5, 0.1))
                .hasMessageContaining("臂越域");
        assertThatThrownBy(() -> Exp3Bandit.probabilityOf(new double[]{1}, 0.1, 1))
                .hasMessageContaining("臂越域");
    }
}
