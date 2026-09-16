package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2042 / T3186：刻度轮合同——轮内延迟到期、跨轮圈数递减、同槽
 * 多任务、游标回绕、幂等重调度、取消、畸形 fail-fast。
 */
class TickWheelTimerTest {

    @Test
    void withinWheelDelayShouldFireExactlyOnTick() {
        TickWheelTimer timer = new TickWheelTimer(16);
        timer.schedule("t5", 5);
        for (int i = 0; i < 4; i++) {
            assertThat(timer.advance()).isEmpty();
        }
        assertThat(timer.advance()).containsExactly("t5"); // 恰第 5 tick
        assertThat(timer.pendingCount()).isZero();
    }

    @Test
    void beyondWheelSpanShouldTrackRounds() {
        TickWheelTimer timer = new TickWheelTimer(8);
        timer.schedule("long", 20); // 20 = 2 圈(16) + 4 槽
        for (int i = 0; i < 19; i++) {
            assertThat(timer.advance()).as("tick " + (i + 1)).isEmpty();
        }
        assertThat(timer.advance()).containsExactly("long"); // 恰第 20 tick
    }

    @Test
    void sameSlotTasksShouldFireTogether() {
        TickWheelTimer timer = new TickWheelTimer(8);
        timer.schedule("a", 3);
        timer.schedule("b", 3);
        timer.schedule("c", 3);
        timer.advance();
        timer.advance();
        List<String> due = timer.advance();
        assertThat(due).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void cursorShouldWrapAround() {
        TickWheelTimer timer = new TickWheelTimer(8);
        for (int i = 0; i < 15; i++) {
            timer.advance(); // 游标走 15/8 圈余 7
        }
        assertThat(timer.ticks()).isEqualTo(15L);
        timer.schedule("after-wrap", 2);
        timer.advance();
        assertThat(timer.advance()).containsExactly("after-wrap"); // 回绕后仍准
    }

    @Test
    void reschedulingSameTaskShouldReplace() {
        TickWheelTimer timer = new TickWheelTimer(16);
        timer.schedule("t", 3);
        timer.schedule("t", 7); // 幂等替换——旧 3 失效
        for (int i = 0; i < 3; i++) {
            assertThat(timer.advance()).isEmpty(); // 第 3 tick 不再触发
        }
        for (int i = 0; i < 3; i++) {
            timer.advance();
        }
        assertThat(timer.advance()).containsExactly("t"); // 第 7 tick
        assertThat(timer.pendingCount()).isZero();
    }

    @Test
    void cancelShouldPreventFiring() {
        TickWheelTimer timer = new TickWheelTimer(16);
        timer.schedule("doomed", 3);
        assertThat(timer.cancel("doomed")).isTrue();
        assertThat(timer.cancel("doomed")).isFalse(); // 再取消 false
        timer.advance();
        timer.advance();
        assertThat(timer.advance()).isEmpty();
        assertThat(timer.pendingCount()).isZero();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new TickWheelTimer(4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TickWheelTimer(100)) // 非 2 的幂
                .isInstanceOf(IllegalArgumentException.class);
        TickWheelTimer timer = new TickWheelTimer();
        assertThatThrownBy(() -> timer.schedule(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.schedule(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.schedule("t", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> timer.cancel(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
