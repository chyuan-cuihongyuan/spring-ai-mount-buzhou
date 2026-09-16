package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2043 / T3188：快速重传触发合同——阈值恰触、切换重计、触发清零
 * 新一轮、首信号不触、账面对账、畸形 fail-fast。
 */
class FastRetransmitTriggerTest {

    @Test
    void thirdDuplicateShouldTrigger() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(3);
        assertThat(trigger.signal("conn-reset")).isFalse(); // 1
        assertThat(trigger.signal("conn-reset")).isFalse(); // 2
        assertThat(trigger.signal("conn-reset")).isTrue();  // 3 —— 恰阈值触发
        assertThat(trigger.triggerCount()).isEqualTo(1L);
    }

    @Test
    void signalSwitchShouldRestartCounting() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(3);
        trigger.signal("a");
        trigger.signal("a");      // a 连续 2
        trigger.signal("b");      // 切换——重计 1
        trigger.signal("a");      // 再切回——重计 1
        trigger.signal("a");      // 2
        assertThat(trigger.signal("a")).isTrue(); // 3 触发
        FastRetransmitTrigger.FastRetransmitStats stats = trigger.stats();
        assertThat(stats.uniqueSignals()).isEqualTo(3L); // a→b→a 两切
        assertThat(stats.dupSignals()).isEqualTo(3L);    // a×2 + a×1（触发前）
    }

    @Test
    void triggerShouldResetForNextRound() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(2);
        assertThat(trigger.signal("x")).isFalse();
        assertThat(trigger.signal("x")).isTrue();  // 触发
        assertThat(trigger.signal("x")).isFalse(); // 新一轮从 1
        assertThat(trigger.signal("x")).isTrue();  // 再触发
        assertThat(trigger.triggerCount()).isEqualTo(2L);
    }

    @Test
    void firstSignalNeverTriggers() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(2);
        assertThat(trigger.signal("solo")).isFalse(); // 首信号计 1
        assertThat(trigger.stats().consecutiveDups()).isEqualTo(1);
    }

    @Test
    void alternatingSignalsShouldNeverTrigger() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(3);
        for (int i = 0; i < 10; i++) {
            assertThat(trigger.signal(i % 2 == 0 ? "a" : "b")).isFalse(); // 交替永不连
        }
        assertThat(trigger.triggerCount()).isZero();
    }

    @Test
    void higherThresholdShouldHoldLonger() {
        FastRetransmitTrigger trigger = new FastRetransmitTrigger(5);
        for (int i = 0; i < 4; i++) {
            assertThat(trigger.signal("z")).isFalse();
        }
        assertThat(trigger.signal("z")).isTrue(); // 恰第 5
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new FastRetransmitTrigger(1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FastRetransmitTrigger(0))
                .isInstanceOf(IllegalArgumentException.class);
        FastRetransmitTrigger trigger = new FastRetransmitTrigger();
        assertThatThrownBy(() -> trigger.signal(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> trigger.signal(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
