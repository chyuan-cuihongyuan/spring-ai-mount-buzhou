package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 823 / T1148：启动阶段耗时回归——begin/end 时长/未结束 -1 哨兵/
 * end 幂等/升序快照/封顶 truncated/空白忽略/fail-fast。
 */
class StartupPhaseTimingTest {

    private static final class MutableClock extends Clock {
        long now = 1_000L;

        void advance(long ms) {
            now += ms;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(now);
        }
    }

    @Test
    void beginEndDurations() {
        MutableClock clock = new MutableClock();
        StartupPhaseTiming timing = new StartupPhaseTiming(clock);

        StartupPhaseTiming.Step stores = timing.start("stores");
        clock.advance(120);
        stores.end();
        clock.advance(30);
        StartupPhaseTiming.Step hooks = timing.start("hooks");
        clock.advance(45);
        hooks.end();

        List<StartupPhaseTiming.StepTiming> snap = timing.snapshot();
        assertThat(snap).hasSize(2);
        assertThat(snap.get(0).phase()).isEqualTo("stores");
        assertThat(snap.get(0).durationMillis()).isEqualTo(120);
        assertThat(snap.get(1).phase()).isEqualTo("hooks");
        assertThat(snap.get(1).durationMillis()).isEqualTo(45);
        assertThat(timing.truncated()).isFalse();
    }

    @Test
    void unfinishedStepSentinelAndEndIdempotent() {
        MutableClock clock = new MutableClock();
        StartupPhaseTiming timing = new StartupPhaseTiming(clock);

        StartupPhaseTiming.Step ongoing = timing.start("ongoing");
        clock.advance(500);
        assertThat(timing.snapshot().get(0).durationMillis()).isEqualTo(-1);

        ongoing.end();
        ongoing.end(); // 幂等——时长按首末计
        clock.advance(500);
        assertThat(timing.snapshot().get(0).durationMillis()).isEqualTo(500);
    }

    @Test
    void stepsCappedAndSortedByStart() {
        MutableClock clock = new MutableClock();
        StartupPhaseTiming timing = new StartupPhaseTiming(clock);
        for (int i = 0; i < StartupPhaseTiming.MAX_STEPS; i++) {
            timing.start("p" + i);
            clock.advance(1);
        }
        assertThat(timing.start("overflow")).isNull();
        assertThat(timing.truncated()).isTrue();
        assertThat(timing.snapshot()).hasSize(StartupPhaseTiming.MAX_STEPS);
        // 升序：首步 p0
        assertThat(timing.snapshot().get(0).phase()).isEqualTo("p0");
    }

    @Test
    void blankPhaseIgnored() {
        StartupPhaseTiming timing = new StartupPhaseTiming(Clock.systemUTC());
        assertThat(timing.start(null)).isNull();
        assertThat(timing.start("  ")).isNull();
        assertThat(timing.snapshot()).isEmpty();
    }

    @Test
    void clockRequired() {
        assertThatThrownBy(() -> new StartupPhaseTiming(null))
                .isInstanceOf(NullPointerException.class);
    }
}
