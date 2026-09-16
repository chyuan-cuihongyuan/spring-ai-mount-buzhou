package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2002 / T3106：指数直方图滑窗计数合同——无跨界精确、窗口滑动
 * 过期清零、确定性估计误差 ≤ errorBound、桶数对数增长（省空间）、
 * 畸形 fail-fast。
 */
class ExponentialWindowCounterTest {

    @Test
    void eventsInsideWindowWithoutStraddlingShouldBeExact() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(10);
        for (int i = 0; i < 5; i++) {
            counter.insert();
            counter.tick();
        }
        // 全部事件仍在窗内且无整体过期桶——估计 = 精确 5
        assertThat(counter.estimate()).isEqualTo(5L);
        assertThat(counter.errorBound()).isEqualTo(0L);
    }

    @Test
    void slidingPastAllEventsShouldDrainToZero() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(4);
        for (int i = 0; i < 3; i++) {
            counter.insert();
            counter.tick();
        }
        assertThat(counter.estimate()).isGreaterThan(0L);
        for (int i = 0; i < 10; i++) {
            counter.tick();
        }
        assertThat(counter.estimate()).isZero();
        assertThat(counter.bucketCount()).isZero();
    }

    @Test
    void burstThenSlideShouldTrackDecayingWindow() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(8);
        // 窗口左端 burst 10 事件，随后空转 8 tick——全部滑出
        for (int i = 0; i < 10; i++) {
            counter.insert();
        }
        assertThat(counter.estimate()).isBetween(5L, 10L); // 爆发瞬间尚在窗内
        for (int i = 0; i < 8; i++) {
            counter.tick();
        }
        assertThat(counter.estimate()).isZero();
    }

    @Test
    void estimateErrorShouldStayWithinSelfDescribedBound() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(16);
        // 确定性序列：事件在 tick 0,3,6,…（insert + 2 tick 间隔），now 终值 3×20−1=59
        for (int i = 0; i < 20; i++) {
            counter.insert();
            counter.tick();
            counter.tick();
        }
        long estimate = counter.estimate();
        long bound = counter.errorBound();
        // 估计与真值（窗内精确数）差 ≤ 自描述界（每跨界桶偏差至多计半份额）
        long exact = exactWindowCount(16, 20);
        assertThat(Math.abs(estimate - exact))
                .as("estimate=%d exact=%d bound=%d", estimate, exact, bound)
                .isLessThanOrEqualTo(bound);
    }

    /** 独立精确重放：事件在 tick 3i（i=0..events−1），now=3×events−1，窗 (now−W, now]。 */
    private static long exactWindowCount(long window, int events) {
        long now = 3L * events - 1;
        long count = 0;
        for (int i = 0; i < events; i++) {
            long ts = 3L * i;
            if (ts > now - window) {
                count++;
            }
        }
        return count;
    }

    @Test
    void bucketCountShouldGrowLogarithmicallyNotLinearly() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(1000);
        for (int i = 0; i < 1000; i++) {
            counter.insert();
        }
        // 1000 事件 → 桶数 ≤ 2×(log2(1000)+1) = 22 量级，远小于 1000
        assertThat(counter.bucketCount()).isLessThanOrEqualTo(24);
        assertThat(counter.estimate()).isEqualTo(1000L); // 无跨界——仍精确
    }

    @Test
    void malformedWindowShouldFailFast() {
        assertThatThrownBy(() -> new ExponentialWindowCounter(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExponentialWindowCounter(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alternatingInsertTickShouldNeverLoseFreshEvents() {
        ExponentialWindowCounter counter = new ExponentialWindowCounter(5);
        for (int i = 0; i < 50; i++) {
            counter.insert();
            counter.tick();
        }
        // 事件在 tick 0..49，now=50，窗 (45,50] → 精确 4（46..49）；
        // 估计与真值的差 ≤ 自描述界（跨界桶计半份额）
        long exact = 4L;
        assertThat(Math.abs(counter.estimate() - exact))
                .as("estimate=%d bound=%d", counter.estimate(), counter.errorBound())
                .isLessThanOrEqualTo(counter.errorBound());
        assertThat(counter.estimate()).isGreaterThanOrEqualTo(exact - counter.errorBound());
    }
}
