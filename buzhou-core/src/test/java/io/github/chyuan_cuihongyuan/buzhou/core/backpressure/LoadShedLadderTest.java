package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1816 / T2834：负载脱落阶梯——逐级甩、边界含上、脱落面读数。 */
class LoadShedLadderTest {

    /** 负载爬升逐级脱落：低阈值先掉、高阈值后掉，未达级仍在服务。 */
    @Test
    void shouldShedProgressivelyAsLoadRises() {
        List<LoadShedLadder.Level> ladder = List.of(
                new LoadShedLadder.Level("batch", 0.5d),
                new LoadShedLadder.Level("interactive", 0.8d),
                new LoadShedLadder.Level("critical", 1.2d));
        LoadShedLadder.ShedDecision healthy = LoadShedLadder.decide(0.3d, ladder);
        assertThat(healthy.escalating()).isFalse();
        assertThat(healthy.shedLevels()).isEmpty();
        assertThat(healthy.keptLevels()).containsExactly("batch", "interactive", "critical");

        LoadShedLadder.ShedDecision rising = LoadShedLadder.decide(0.6d, ladder);
        assertThat(rising.shedLevels()).containsExactly("batch");
        assertThat(rising.keptLevels()).containsExactly("interactive", "critical");
        assertThat(rising.shedRatio()).isEqualTo(1.0 / 3.0);

        LoadShedLadder.ShedDecision overload = LoadShedLadder.decide(1.5d, ladder);
        assertThat(overload.shedLevels()).containsExactly("batch", "interactive", "critical");
        assertThat(overload.shedRatio()).isEqualTo(1.0d);
    }

    /** 阈值边界含上：loadFactor == threshold 即脱落。 */
    @Test
    void thresholdBoundaryIsInclusive() {
        LoadShedLadder.ShedDecision atEdge = LoadShedLadder.decide(0.8d, List.of(
                new LoadShedLadder.Level("interactive", 0.8d)));
        assertThat(atEdge.shedLevels()).containsExactly("interactive");
        assertThat(atEdge.escalating()).isTrue();
    }

    /** 空表与 null 同口径：无级可甩、比率 -1 哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (LoadShedLadder.ShedDecision d : List.of(
                LoadShedLadder.decide(1.0d, List.of()),
                LoadShedLadder.decide(1.0d, null))) {
            assertThat(d.shedLevels()).isEmpty();
            assertThat(d.keptLevels()).isEmpty();
            assertThat(d.shedRatio()).isEqualTo(-1d);
            assertThat(d.escalating()).isFalse();
        }
    }

    /** 畸形入参 fail-fast：负/NaN 负载因子、空白名、负阈值。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> LoadShedLadder.decide(-0.1d, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loadFactor 须 ≥ 0");
        assertThatThrownBy(() -> LoadShedLadder.decide(Double.NaN, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoadShedLadder.Level("", 0.5d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoadShedLadder.Level("x", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
