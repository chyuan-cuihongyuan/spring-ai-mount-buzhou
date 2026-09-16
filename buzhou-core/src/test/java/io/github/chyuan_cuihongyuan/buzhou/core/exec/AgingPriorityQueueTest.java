package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2024 / T3150：老化优先级合同——基础序、等待生息反超（反饥饿）、
 * 同分 FIFO、零速率退化静态、快照不出队、畸形 fail-fast。
 */
class AgingPriorityQueueTest {

    @Test
    void higherBasePriorityShouldPollFirst() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(0.0d); // 静态
        queue.enqueue("low", 10, 0);
        queue.enqueue("high", 90, 0);
        assertThat(queue.poll(0)).isEqualTo("high");
        assertThat(queue.poll(0)).isEqualTo("low");
    }

    @Test
    void waitingLongEnoughShouldOvertakeNewcomer() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(1.0d); // 1 点/秒
        queue.enqueue("old-low", 10, 0);
        // 90 秒后：old-low 有效 = 10 + 90 = 100
        queue.enqueue("new-high", 90, 90_000);
        assertThat(queue.poll(90_000)).isEqualTo("old-low"); // 100 > 90——反饥饿反超
        assertThat(queue.poll(90_000)).isEqualTo("new-high");
    }

    @Test
    void newcomerShouldStillWinWhenGapTooBig() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(1.0d);
        queue.enqueue("old-low", 10, 0);
        queue.enqueue("new-high", 90, 10_000); // 10 秒后老者 20 < 90
        assertThat(queue.poll(10_000)).isEqualTo("new-high");
        assertThat(queue.poll(10_000)).isEqualTo("old-low");
    }

    @Test
    void equalEffectiveScoreShouldPollInEnqueueOrder() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(1.0d);
        queue.enqueue("first", 50, 0);
        queue.enqueue("second", 60, 10_000); // 10 秒后两者有效同 60
        assertThat(queue.poll(10_000)).isEqualTo("first"); // 同分 FIFO
        assertThat(queue.poll(10_000)).isEqualTo("second");
    }

    @Test
    void zeroRateShouldDegradeToStaticPriority() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(0.0d);
        queue.enqueue("old-low", 10, 0);
        queue.enqueue("new-high", 20, 1_000_000); // 等多久都不涨
        assertThat(queue.poll(1_000_000)).isEqualTo("new-high");
    }

    @Test
    void emptyPollShouldReturnNull() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>();
        assertThat(queue.poll(0)).isNull();
        assertThat(queue.size()).isZero();
    }

    @Test
    void snapshotShouldNotMutateQueue() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>(1.0d);
        queue.enqueue("a", 10, 0);       // 老住户：t=100s 有效 10+100=110
        queue.enqueue("b", 90, 90_000);  // 新来者：t=100s 有效 90+10=100
        List<String> snapshot = queue.snapshotByEffectivePriority(100_000);
        assertThat(snapshot).containsExactly("a", "b"); // 110 > 100——老化反超
        assertThat(queue.size()).isEqualTo(2); // 快照不出队
        assertThat(queue.poll(100_000)).isEqualTo("a");
    }

    @Test
    void malformedInputsShouldFailFast() {
        AgingPriorityQueue<String> queue = new AgingPriorityQueue<>();
        assertThatThrownBy(() -> new AgingPriorityQueue<>(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.enqueue(null, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.enqueue("x", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.enqueue("x", 101, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.enqueue("x", 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> queue.poll(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
