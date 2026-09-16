package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2032 / T3166：UCB1 合同——未试臂优先、探索半径收缩、收敛最优
 * 臂、冷落臂不被判死刑（持续小份额）、零探索退化贪心、畸形 fail-fast。
 */
class Ucb1SelectorTest {

    @Test
    void untriedArmsShouldBeTriedFirst() {
        Ucb1Selector selector = new Ucb1Selector();
        selector.registerArm("a");
        selector.registerArm("b");
        selector.registerArm("c");
        // 未试臂优先：三臂各被选一次后才进 UCB 阶段
        assertThat(selector.selectArm()).isIn("a", "b", "c");
        selector.recordReward(selector.selectArm(), 1.0);
        selector.recordReward(selector.selectArm(), 0.0);
        selector.recordReward(selector.selectArm(), 0.5);
        assertThat(selector.armPulls().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(3L); // 每臂恰一试
    }

    @Test
    void bestArmShouldWinInLongRun() {
        Ucb1Selector selector = new Ucb1Selector();
        selector.registerArm("good");
        selector.registerArm("bad");
        // 模拟 200 轮：选臂→按真值带奖励（good≈0.9，bad≈0.1，无方差简化）
        for (int i = 0; i < 200; i++) {
            String arm = selector.selectArm();
            selector.recordReward(arm, arm.equals("good") ? 0.9 : 0.1);
        }
        Map<String, Long> pulls = selector.armPulls();
        assertThat(pulls.get("good")).isGreaterThan(pulls.get("bad")); // exploit 主导
        assertThat(pulls.get("bad")).isGreaterThan(0L); // 但未被冷落——探索仍在
    }

    @Test
    void unluckyEarlyDrawShouldNotStarveArm() {
        // good 臂首次不幸低奖励（估算未收敛）——UCB 半径仍会再探它
        Ucb1Selector selector = new Ucb1Selector();
        selector.registerArm("good");
        selector.registerArm("mediocre");
        selector.recordReward("good", 0.0);    // 不幸首抽
        selector.recordReward("mediocre", 0.8);
        // 接下来若干选择中 good 必被再探（半径大）
        boolean goodRetried = false;
        for (int i = 0; i < 10; i++) {
            if (selector.selectArm().equals("good")) {
                goodRetried = true;
                selector.recordReward("good", 0.9); // 真值显形
            } else {
                selector.recordReward("mediocre", 0.8);
            }
        }
        assertThat(goodRetried).isTrue(); // 未判死刑
        assertThat(selector.armMeans().get("good")).isGreaterThan(0.0);
    }

    @Test
    void zeroExplorationShouldDegradeToGreedy() {
        Ucb1Selector selector = new Ucb1Selector(0.0);
        selector.registerArm("best");
        selector.registerArm("worse");
        selector.recordReward("best", 1.0);
        selector.recordReward("worse", 0.5);
        // 纯均值贪心：永远选 best（worse 饿死——exploration=0 的显式退化）
        for (int i = 0; i < 5; i++) {
            assertThat(selector.selectArm()).isEqualTo("best");
        }
    }

    @Test
    void emptySelectorShouldReturnNull() {
        assertThat(new Ucb1Selector().selectArm()).isNull();
    }

    @Test
    void armMeansShouldTrackRewards() {
        Ucb1Selector selector = new Ucb1Selector();
        selector.registerArm("a");
        selector.recordReward("a", 1.0);
        selector.recordReward("a", 0.5);
        assertThat(selector.armMeans().get("a")).isEqualTo(0.75d);
        assertThat(selector.armPulls().get("a")).isEqualTo(2L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        Ucb1Selector selector = new Ucb1Selector();
        assertThatThrownBy(() -> new Ucb1Selector(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> selector.registerArm(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> selector.registerArm(" "))
                .isInstanceOf(IllegalArgumentException.class);
        selector.registerArm("a");
        assertThatThrownBy(() -> selector.registerArm("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已注册");
        assertThatThrownBy(() -> selector.recordReward("ghost", 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> selector.recordReward("a", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> selector.recordReward("a", 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
