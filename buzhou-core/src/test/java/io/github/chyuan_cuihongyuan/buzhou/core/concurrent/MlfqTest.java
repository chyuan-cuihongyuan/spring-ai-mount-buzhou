package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6028：Mlfq 合同——多级队列+量子递增+用满降级。
 * 长任务逐级沉底+量子翻倍；新任务插队最高级；同级 FIFO；
 * 完成移出；fail-fast。
 */
class MlfqTest {

    @Test
    void longTaskDemotesWithGrowingQuantum() {
        Mlfq mlfq = new Mlfq(3, 2);
        mlfq.submit(100);
        Mlfq.Dispatch first = mlfq.dispatch();
        assertThat(first.level()).isZero();
        assertThat(first.quantum()).isEqualTo(2);
        mlfq.onQuantumExpired(100);
        Mlfq.Dispatch second = mlfq.dispatch();
        assertThat(second.level()).isEqualTo(1);
        assertThat(second.quantum()).isEqualTo(4);
        mlfq.onQuantumExpired(100);
        Mlfq.Dispatch third = mlfq.dispatch();
        assertThat(third.level()).isEqualTo(2);
        assertThat(third.quantum()).isEqualTo(8);
        mlfq.onQuantumExpired(100);
        Mlfq.Dispatch fourth = mlfq.dispatch();
        assertThat(fourth.level()).as("底级不再降").isEqualTo(2);
    }

    @Test
    void freshTaskPreemptsDemotedOnes() {
        Mlfq mlfq = new Mlfq(3, 2);
        mlfq.submit(1);
        mlfq.dispatch();
        mlfq.onQuantumExpired(1);
        assertThat(mlfq.readyAt(1)).isEqualTo(1);
        mlfq.submit(2);
        Mlfq.Dispatch dispatch = mlfq.dispatch();
        assertThat(dispatch.taskId()).as("新任务在最高级先派").isEqualTo(2);
        mlfq.onCompleted(2);
        Mlfq.Dispatch demoted = mlfq.dispatch();
        assertThat(demoted.taskId()).isEqualTo(1);
        mlfq.onCompleted(1);
        assertThat(mlfq.activeTasks()).isZero();
    }

    @Test
    void sameLevelRunsFifo() {
        Mlfq mlfq = new Mlfq(2, 4);
        mlfq.submit(10);
        mlfq.submit(20);
        mlfq.submit(30);
        assertThat(mlfq.dispatch().taskId()).isEqualTo(10);
        mlfq.onCompleted(10);
        assertThat(mlfq.dispatch().taskId()).isEqualTo(20);
        mlfq.onCompleted(20);
        assertThat(mlfq.dispatch().taskId()).isEqualTo(30);
    }

    @Test
    void completedTaskLeavesSystem() {
        Mlfq mlfq = new Mlfq(2, 4);
        mlfq.submit(1);
        mlfq.submit(2);
        assertThat(mlfq.dispatch().taskId()).isEqualTo(1);
        mlfq.onCompleted(1);
        assertThat(mlfq.dispatch().taskId()).isEqualTo(2);
        mlfq.onQuantumExpired(2);
        assertThat(mlfq.activeTasks()).isEqualTo(1);
        assertThat(mlfq.readyAt(0)).isZero();
        assertThat(mlfq.readyAt(1)).isEqualTo(1);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new Mlfq(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mlfq(17, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Mlfq(2, 0)).isInstanceOf(IllegalArgumentException.class);
        Mlfq mlfq = new Mlfq(2, 4);
        assertThat(mlfq.dispatch()).isNull();
        mlfq.submit(1);
        assertThatThrownBy(() -> mlfq.submit(1)).isInstanceOf(IllegalArgumentException.class);
        mlfq.dispatch();
        assertThatThrownBy(() -> mlfq.onCompleted(99)).isInstanceOf(IllegalArgumentException.class);
        mlfq.onQuantumExpired(1);
        assertThatThrownBy(() -> mlfq.onQuantumExpired(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mlfq.readyAt(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
