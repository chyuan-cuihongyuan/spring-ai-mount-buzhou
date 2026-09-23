package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4025 / T6052：重启强度合同——窗内不越、越限闩锁、
 * 滑窗淘汰、新纪元、畸形 fail-fast。
 */
class SupervisorRestartIntensityTest {

    @Test
    void withinIntensityShouldNotEscalate() {
        long[] tick = {0};
        SupervisorRestartIntensity sup = new SupervisorRestartIntensity(3, 100, () -> tick[0]);
        for (int i = 0; i < 3; i++) {
            sup.onRestart();
        }
        assertThat(sup.restartsInWindow()).isEqualTo(3);   // 恰满不越
        assertThat(sup.exceeded()).isFalse();
        assertThat(sup.maxRestarts()).isEqualTo(3);
    }

    @Test
    void exceedingIntensityShouldLatchEscalation() {
        long[] tick = {0};
        SupervisorRestartIntensity sup = new SupervisorRestartIntensity(3, 100, () -> tick[0]);
        for (int i = 0; i < 4; i++) {   // 窗内第 4 次——越限
            sup.onRestart();
        }
        assertThat(sup.exceeded()).isTrue();
        tick[0] = 10_000;   // 窗早已滑走——闩锁仍保持（升级是事实）
        assertThat(sup.exceeded()).isTrue();
        assertThat(sup.restartsInWindow()).isZero();
    }

    @Test
    void slidingWindowShouldExpireOldRestarts() {
        long[] tick = {0};
        SupervisorRestartIntensity sup = new SupervisorRestartIntensity(3, 100, () -> tick[0]);
        sup.onRestart();   // t=0
        tick[0] = 10;
        sup.onRestart();   // t=10
        tick[0] = 20;
        sup.onRestart();   // t=20——恰满
        assertThat(sup.exceeded()).isFalse();
        tick[0] = 150;   // 窗=100：now−t ≥ 100 全淘汰（0/10/20 均出窗）
        assertThat(sup.restartsInWindow()).isZero();
        sup.onRestart();   // t=150——新窗重新起算 1/3
        assertThat(sup.restartsInWindow()).isEqualTo(1);
        assertThat(sup.exceeded()).isFalse();
    }

    @Test
    void resetShouldStartNewEpoch() {
        long[] tick = {0};
        SupervisorRestartIntensity sup = new SupervisorRestartIntensity(2, 100, () -> tick[0]);
        sup.onRestart();
        sup.onRestart();
        sup.onRestart();   // 越限
        assertThat(sup.exceeded()).isTrue();
        sup.reset();   // 监督者自身重启——新纪元
        assertThat(sup.exceeded()).isFalse();
        assertThat(sup.restartsInWindow()).isZero();
        sup.onRestart();
        assertThat(sup.exceeded()).isFalse();   // 新纪元从头计
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SupervisorRestartIntensity(0, 100, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SupervisorRestartIntensity(3, 0, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SupervisorRestartIntensity(3, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
