package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1858 / T2918：频次衰减——减半封底、顶替周期、不可达哨兵。 */
class FrequencyDecayTest {

    /** 衰减：每周期减半向下取整、封底 0（多周期仍 0）。 */
    @Test
    void shouldHalvePerPeriodWithFloor() {
        assertThat(FrequencyDecay.decayed(100, 1)).isEqualTo(50);
        assertThat(FrequencyDecay.decayed(100, 2)).isEqualTo(25);
        assertThat(FrequencyDecay.decayed(100, 3)).isEqualTo(12);
        assertThat(FrequencyDecay.decayed(1, 1)).isZero();
        assertThat(FrequencyDecay.decayed(100, 1000)).isZero();
        assertThat(FrequencyDecay.decayed(100, 0)).isEqualTo(100);
    }

    /** 顶替周期：老 100 衰减、新 5 命中/周期——t=3 时 15>12 顶替。 */
    @Test
    void shouldSolveOvertakePeriod() {
        // t=1: 5>50 否；t=2: 10>25 否；t=3: 15>12 是
        assertThat(FrequencyDecay.overtakePeriod(100, 5)).isEqualTo(3);
        // 热 newcomer（每周期 60 命中）第 1 周期即 60>50
        assertThat(FrequencyDecay.overtakePeriod(100, 60)).isEqualTo(1);
        // 冷 newcomer（1/周期）：t=5 时 5 > decayed(100,5)=3 顶替
        assertThat(FrequencyDecay.overtakePeriod(100, 1)).isEqualTo(5);
    }

    /** 零计数即刻被顶替；零命中永不过顶。 */
    @Test
    void zeroCasesBehave() {
        assertThat(FrequencyDecay.overtakePeriod(0, 1)).isEqualTo(1);
        assertThat(FrequencyDecay.overtakePeriod(100, 0)).isEqualTo(-1);
    }

    /** 畸形入参 fail-fast：负计数/负命中。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> FrequencyDecay.decayed(-1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入参不能为负");
        assertThatThrownBy(() -> FrequencyDecay.decayed(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FrequencyDecay.overtakePeriod(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FrequencyDecay.overtakePeriod(1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
