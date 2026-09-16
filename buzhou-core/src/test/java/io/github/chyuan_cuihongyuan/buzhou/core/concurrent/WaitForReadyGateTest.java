package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.WaitForReadyGate.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2021 / T3144：就绪等待门合同——双态四结局、预算满拒绝、就绪
 * 批量排空、翻回未就绪重计、计数对账、畸形 fail-fast。
 */
class WaitForReadyGateTest {

    @Test
    void notReadyShouldFailFastOrQueueByPosture() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 10);
        assertThat(gate.tryAcquire(false)).isEqualTo(Outcome.FAIL_FAST); // 立即失败
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUED);    // 排队等待
        assertThat(gate.queueDepth()).isEqualTo(1);
    }

    @Test
    void readyShouldPassWithoutQueueing() {
        WaitForReadyGate gate = new WaitForReadyGate(true, 10);
        assertThat(gate.tryAcquire(false)).isEqualTo(Outcome.PASS);
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.PASS); // 就绪下姿态无关
        assertThat(gate.queueDepth()).isZero();
        assertThat(gate.stats().passes()).isEqualTo(2L);
    }

    @Test
    void queueBudgetShouldRejectWhenFull() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 2);
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUED);
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUED);
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUE_FULL); // 预算满
        assertThat(gate.queueDepth()).isEqualTo(2); // 深度不超预算
        assertThat(gate.stats().queueFullRejections()).isEqualTo(1L);
    }

    @Test
    void zeroBudgetShouldRejectAllQueuers() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 0);
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUE_FULL); // 零预算=禁排队
        assertThat(gate.tryAcquire(false)).isEqualTo(Outcome.FAIL_FAST);
    }

    @Test
    void turningReadyShouldDrainQueueInBatch() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 10);
        gate.tryAcquire(true);
        gate.tryAcquire(true);
        gate.tryAcquire(true);
        gate.setReady(true); // 批量排空
        assertThat(gate.queueDepth()).isZero();
        assertThat(gate.stats().drained()).isEqualTo(3L); // 三请求放行
        assertThat(gate.stats().drainBatches()).isEqualTo(1L);
        assertThat(gate.tryAcquire(false)).isEqualTo(Outcome.PASS); // 此后直通
    }

    @Test
    void readyToNotReadyShouldRestartQueueAccounting() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 10);
        gate.tryAcquire(true); // 排队 1
        gate.setReady(true);   // 排空
        gate.setReady(false);  // 再失就绪
        assertThat(gate.queueDepth()).isZero(); // 新队重计
        assertThat(gate.tryAcquire(true)).isEqualTo(Outcome.QUEUED);
        assertThat(gate.stats().drained()).isEqualTo(1L); // 排空账不重复计
    }

    @Test
    void setReadySameValueShouldBeNoOpForDrainAccounting() {
        WaitForReadyGate gate = new WaitForReadyGate(false, 10);
        gate.tryAcquire(true);
        gate.setReady(false); // 同值翻转——不排空
        assertThat(gate.queueDepth()).isEqualTo(1);
        assertThat(gate.stats().drained()).isZero();
    }

    @Test
    void malformedBudgetShouldFailFast() {
        assertThatThrownBy(() -> new WaitForReadyGate(false, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
