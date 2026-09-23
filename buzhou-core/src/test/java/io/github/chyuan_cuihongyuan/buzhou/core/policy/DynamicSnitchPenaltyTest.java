package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4028 / T6058：动态 snitch 合同——慢罚快免、EWMA 平滑、
 * 恢复免罚、排序推尾、畸形 fail-fast。
 */
class DynamicSnitchPenaltyTest {

    @Test
    void slowReplicaShouldBePenalizedAndRankedLast() {
        DynamicSnitchPenalty snitch = new DynamicSnitchPenalty(1.5, 100.0);
        snitch.record("fast", 10);
        snitch.record("fast", 10);
        snitch.record("slow", 50);
        snitch.record("slow", 50);
        assertThat(snitch.penalized("fast")).isFalse();
        assertThat(snitch.penalized("slow")).isTrue();   // 50 > 10×1.5
        assertThat(snitch.scoreOf("fast")).isEqualTo(10.0);
        assertThat(snitch.scoreOf("slow")).isEqualTo(150.0);   // 50+100 罚分
        assertThat(snitch.ranking()).containsExactly("fast", "slow");
        assertThat(snitch.threshold()).isEqualTo(1.5);
    }

    @Test
    void ewmaShouldSmoothTowardRecentObservations() {
        DynamicSnitchPenalty snitch = new DynamicSnitchPenalty(1.5, 100.0);
        snitch.record("r", 100);
        double previous = snitch.ewmaOf("r");
        for (int i = 0; i < 5; i++) {
            snitch.record("r", 10);
            double current = snitch.ewmaOf("r");
            assertThat(current).isLessThan(previous);   // 单调收敛向新观测
            previous = current;
        }
        assertThat(snitch.ewmaOf("r")).isBetween(10.0, 100.0);
    }

    @Test
    void recoveredReplicaShouldLosePenalty() {
        DynamicSnitchPenalty snitch = new DynamicSnitchPenalty(1.5, 100.0);
        snitch.record("peer", 10);
        snitch.record("r", 60);
        assertThat(snitch.penalized("r")).isTrue();   // 60 > 10×1.5
        for (int i = 0; i < 10; i++) {
            snitch.record("r", 4);   // 连续快观测——EWMA 回落
        }
        assertThat(snitch.penalized("r")).isFalse();   // 恢复免罚自动复用
        assertThat(snitch.scoreOf("r")).isEqualTo(snitch.ewmaOf("r"));
    }

    @Test
    void unseenReplicaShouldBeHonestNaN() {
        DynamicSnitchPenalty snitch = new DynamicSnitchPenalty(1.5, 100.0);
        snitch.record("known", 10);
        assertThat(snitch.ewmaOf("unknown")).isNaN();
        assertThat(snitch.scoreOf("unknown")).isNaN();
        assertThat(snitch.penalized("unknown")).isFalse();   // 零知识不罚
        assertThat(snitch.ranking()).containsExactly("known");   // 只排已观测
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new DynamicSnitchPenalty(1.0, 100.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DynamicSnitchPenalty(1.5, -1))
                .isInstanceOf(IllegalArgumentException.class);
        DynamicSnitchPenalty snitch = new DynamicSnitchPenalty(1.5, 100.0);
        assertThatThrownBy(() -> snitch.record(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snitch.record("r", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
