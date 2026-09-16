package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2033 / T3168：维护触发合同——比例阈值（含恰达界）、全死必清、
 * 双零不触、低流量间隔兜底、触发记账、畸形 fail-fast。
 */
class MaintenanceTriggerTest {

    @Test
    void belowThresholdShouldNotTriggerByRatio() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, Long.MAX_VALUE);
        trigger.noteTriggered(0); // 锚定间隔基点
        assertThat(trigger.shouldTrigger(10, 100, 1_000)).isFalse(); // 10% < 20%
        assertThat(trigger.shouldTrigger(19, 100, 1_000)).isFalse();
    }

    @Test
    void exactlyAtThresholdShouldTrigger() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, Long.MAX_VALUE);
        trigger.noteTriggered(0);
        assertThat(trigger.shouldTrigger(20, 100, 1_000)).isTrue(); // 恰 20%（>= 语义）
        assertThat(trigger.shouldTrigger(50, 100, 1_000)).isTrue();
    }

    @Test
    void allDeadShouldTriggerEvenWithoutLive() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, Long.MAX_VALUE);
        trigger.noteTriggered(0);
        assertThat(trigger.shouldTrigger(5, 0, 1_000)).isTrue(); // live=0 全死必清
        assertThat(trigger.shouldTrigger(0, 0, 1_000)).isFalse(); // 双零无事可做
    }

    @Test
    void lowTrafficIntervalShouldEventuallyTrigger() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, 1_000L);
        trigger.noteTriggered(0);
        assertThat(trigger.shouldTrigger(0, 100, 500)).isFalse();  // 比例未达且间隔未到
        assertThat(trigger.shouldTrigger(0, 100, 1_000)).isTrue(); // 间隔兜底触发
    }

    @Test
    void neverTriggeredShouldUseZeroAsBasepoint() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, 1_000L);
        assertThat(trigger.lastTriggeredAt()).isEqualTo(-1L);
        assertThat(trigger.shouldTrigger(0, 100, 1_000)).isTrue(); // 从未触发自 0 起算
    }

    @Test
    void triggeringShouldResetIntervalAccounting() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(0.2d, 1_000L);
        assertThat(trigger.shouldTrigger(0, 100, 1_000)).isTrue(); // 间隔触发
        trigger.noteTriggered(1_000);
        assertThat(trigger.shouldTrigger(0, 100, 1_500)).isFalse(); // 间隔重新起算
        assertThat(trigger.shouldTrigger(0, 100, 2_000)).isTrue();
        assertThat(trigger.triggerCount()).isEqualTo(1L);
        assertThat(trigger.lastTriggeredAt()).isEqualTo(1_000L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new MaintenanceTrigger(0, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MaintenanceTrigger(1.1, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MaintenanceTrigger(0.2, 0))
                .isInstanceOf(IllegalArgumentException.class);
        MaintenanceTrigger trigger = new MaintenanceTrigger();
        assertThatThrownBy(() -> trigger.shouldTrigger(-1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> trigger.shouldTrigger(0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> trigger.noteTriggered(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
