package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1882 / T2966：配额告警门——写满触发、读放行、显式恢复、畸形。 */
class QuotaAlarmGateTest {

    /** 写满即拒且保持 triggered；告警前写照常。 */
    @Test
    void writeFullTriggersNoSpace() {
        QuotaAlarmGate gate = new QuotaAlarmGate(1000);
        assertThat(gate.onWrite(600)).isTrue();
        assertThat(gate.writesBlocked()).isFalse();
        assertThat(gate.onWrite(500)).isFalse(); // 600+500>1000 → 触发并拒绝
        assertThat(gate.writesBlocked()).isTrue();
        assertThat(gate.onWrite(1)).isFalse(); // 保持拒绝
    }

    /** 读路径永放行——etcd 只读语义核心。恰好写满不告警（etcd：下一笔装不下才触发）。 */
    @Test
    void readsAlwaysAllowed() {
        QuotaAlarmGate gate = new QuotaAlarmGate(100);
        assertThat(gate.onWrite(100)).isTrue();
        assertThat(gate.writesBlocked()).isFalse();
        assertThat(gate.onWrite(1)).isFalse(); // 装不下 → 触发
        assertThat(gate.writesBlocked()).isTrue();
        assertThat(gate.readsAllowed()).isTrue();
    }

    /** ack：释放不足额保持拒绝，足额复位。 */
    @Test
    void acknowledgeRequiresFreeThreshold() {
        QuotaAlarmGate gate = new QuotaAlarmGate(1000);
        assertThat(gate.onWrite(1000)).isTrue(); // 写满（不告警）
        assertThat(gate.onWrite(1)).isFalse(); // 下一笔装不下 → 触发
        gate.onRelease(500);
        assertThat(gate.acknowledge(600)).isFalse(); // 空闲 500 < 600
        assertThat(gate.writesBlocked()).isTrue();
        assertThat(gate.acknowledge(500)).isTrue(); // 空闲 500 ≥ 500
        assertThat(gate.writesBlocked()).isFalse();
        assertThat(gate.onWrite(400)).isTrue(); // 复位后恢复写
    }

    /** 水位读数精确；畸形三型 fail-fast。 */
    @Test
    void usageRatioAndMalformed() {
        QuotaAlarmGate gate = new QuotaAlarmGate(1000);
        gate.onWrite(500);
        assertThat(gate.usageRatio()).isCloseTo(0.5, within(1e-12));
        gate.onWrite(470);
        assertThat(gate.usageRatio()).isCloseTo(0.97, within(1e-12));
        assertThatThrownBy(() -> new QuotaAlarmGate(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quotaBytes 不能小于 1");
        assertThatThrownBy(() -> gate.onWrite(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("写入字节数不能为负");
        assertThatThrownBy(() -> gate.acknowledge(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minFreeBytes 不能为负");
    }
}
