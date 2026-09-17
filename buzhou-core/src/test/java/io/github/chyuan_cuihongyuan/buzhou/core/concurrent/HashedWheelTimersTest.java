package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.HashedWheelTimers.Timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3030 / T5062：时间轮合同——到期边界（未到不出/恰到即出）、
 * 多轮滞槽（延迟超轮周长）、槽回绕、同刻按 id 序、守恒恒等
 * （scheduled==fired+pending）、粒度诚实边界（delay 0 下一 tick）、
 * 随机压力对拍朴素清单、参数校验。
 */
class HashedWheelTimersTest {

    @Test
    void deadlineBoundaryShouldBeExact() {
        HashedWheelTimers wheel = new HashedWheelTimers(8, 50);
        wheel.schedule(0, 150, "t");
        assertThat(wheel.advanceTo(100)).isEmpty();     // 未到
        assertThat(wheel.pending()).isEqualTo(1);
        List<Timer> fired = wheel.advanceTo(150);       // 恰到
        assertThat(fired).hasSize(1);
        assertThat(fired.get(0).label()).isEqualTo("t");
        assertThat(wheel.advanceTo(1_000)).isEmpty();   // 已出不重出
        assertThat(wheel.pending()).isZero();
    }

    @Test
    void delayBeyondWheelSpanShouldStayUntilItsRound() {
        // 轮周长 8×10=80ms，延迟 300 → 4 轮后槽重访才出
        HashedWheelTimers wheel = new HashedWheelTimers(8, 10);
        wheel.schedule(0, 300, "long");
        assertThat(wheel.advanceTo(240)).isEmpty();
        assertThat(wheel.pending()).isEqualTo(1);
        List<Timer> fired = wheel.advanceTo(300);
        assertThat(fired).extracting(Timer::label).containsExactly("long");
    }

    @Test
    void slotsShouldWrapAround() {
        HashedWheelTimers wheel = new HashedWheelTimers(4, 10);
        wheel.schedule(0, 45, "a");   // tick 4 → slot 0
        wheel.schedule(0, 95, "b");   // tick 9 → slot 1（回绕后）
        assertThat(wheel.advanceTo(45)).extracting(Timer::label).containsExactly("a");
        assertThat(wheel.advanceTo(95)).extracting(Timer::label).containsExactly("b");
    }

    @Test
    void sameDeadlineShouldFireInIdOrder() {
        HashedWheelTimers wheel = new HashedWheelTimers(8, 10);
        wheel.schedule(0, 100, "first");
        wheel.schedule(0, 100, "second");
        wheel.schedule(0, 90, "earlier");
        List<Timer> fired = wheel.advanceTo(100);
        assertThat(fired).extracting(Timer::label)
                .containsExactly("earlier", "first", "second");
    }

    @Test
    void conservationShouldHoldUnderRandomPressure() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(21);
        HashedWheelTimers wheel = new HashedWheelTimers(16, 5);
        long now = 0;
        for (int op = 0; op < 2_000; op++) {
            now += rng.nextInt(8);
            if (rng.nextBoolean()) {
                wheel.schedule(now, rng.nextInt(0, 200), "t" + op);
            } else {
                wheel.advanceTo(now);
            }
            assertThat(wheel.scheduledCount()).as("op %d 守恒", op)
                    .isEqualTo(wheel.firedCount() + wheel.pending());
        }
        wheel.advanceTo(now + 1_000);
        assertThat(wheel.pending()).isZero();
        assertThat(wheel.firedCount()).isEqualTo(wheel.scheduledCount());
    }

    @Test
    void zeroDelayFiresWithinOneWheelSpan() {
        // 粒度诚实边界：到期校验在槽重访时——deadline 过后最多再等
        // 一个轮周长（4×10=40ms）。轮同步后 delay 0：槽在 tick 10 已
        // 掠过 → tick 14（=140ms）重访才出，且必在一轮周长内
        HashedWheelTimers wheel = new HashedWheelTimers(4, 10);
        wheel.advanceTo(100);   // 轮同步（currentTick=10）
        wheel.schedule(100, 0, "instant");
        assertThat(wheel.advanceTo(109)).isEmpty();
        assertThat(wheel.advanceTo(140)).hasSize(1);   // 槽重访出
        assertThat(wheel.pending()).isZero();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new HashedWheelTimers(1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HashedWheelTimers(8, 0)).isInstanceOf(IllegalArgumentException.class);
        HashedWheelTimers wheel = new HashedWheelTimers(8, 10);
        assertThatThrownBy(() -> wheel.schedule(0, -1, "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
