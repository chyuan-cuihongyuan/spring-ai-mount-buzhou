package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1827 / T2856：排空预测——两约束取大、主导方读数、瓶颈定位。 */
class DrainForecastTest {

    /** 单会话主导（workBound）：一个胖子拖死，makespan=其剩余。 */
    @Test
    void workBoundSessionDominatesMakespan() {
        DrainForecast.Forecast f = DrainForecast.forecast(4, 10L, List.of(
                new DrainForecast.SessionWork("fat", 1000),
                new DrainForecast.SessionWork("s1", 10),
                new DrainForecast.SessionWork("s2", 10)));
        assertThat(f.totalRemainingUnits()).isEqualTo(1020L);
        assertThat(f.parallelismBound()).isFalse();
        assertThat(f.bottleneckSession()).isEqualTo("fat");
        assertThat(f.makespanMillis()).isEqualTo(10_000L);
    }

    /** 并行度主导（parallelismBound）：总量压垮并行，ceil 商决定。 */
    @Test
    void parallelismBoundWhenTotalOverwhelms() {
        DrainForecast.Forecast f = DrainForecast.forecast(2, 1L, List.of(
                new DrainForecast.SessionWork("a", 10),
                new DrainForecast.SessionWork("b", 10),
                new DrainForecast.SessionWork("c", 10),
                new DrainForecast.SessionWork("d", 11)));
        // 总 41 ÷ 2 → ceil 21 > 最大 11 → 并行主导；并列瓶颈取首个（a 先达 10？d 是 11——瓶颈 d）
        assertThat(f.parallelismBound()).isTrue();
        assertThat(f.bottleneckSession()).isEqualTo("d");
        assertThat(f.makespanMillis()).isEqualTo(21L);
    }

    /** 相等即单会话主导（并列取更可操作处方——催单点）。 */
    @Test
    void tieDefaultsToWorkBound() {
        DrainForecast.Forecast f = DrainForecast.forecast(2, 1L, List.of(
                new DrainForecast.SessionWork("a", 10),
                new DrainForecast.SessionWork("b", 10)));
        assertThat(f.parallelismBound()).isFalse();
        assertThat(f.makespanMillis()).isEqualTo(10L);
    }

    /** 空排空与 null：零时长、无瓶颈。 */
    @Test
    void emptyAndNullYieldZeroForecast() {
        for (DrainForecast.Forecast f : List.of(
                DrainForecast.forecast(4, 10L, List.of()),
                DrainForecast.forecast(4, 10L, null))) {
            assertThat(f.sessions()).isZero();
            assertThat(f.makespanMillis()).isZero();
            assertThat(f.bottleneckSession()).isNull();
            assertThat(f.parallelismBound()).isFalse();
        }
    }

    /** 畸形入参 fail-fast：并行度 < 1、负耗时、空白 id/负剩余。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> DrainForecast.forecast(0, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parallelism 不能小于 1");
        assertThatThrownBy(() -> DrainForecast.forecast(1, -1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrainForecast.SessionWork("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrainForecast.SessionWork("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
