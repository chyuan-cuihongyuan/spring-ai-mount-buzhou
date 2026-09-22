package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1912 / T3026：探测预算——占比、判定两态、畸形。 */
class ProbeBudgetTest {

    /** 占比读数：50/1000 = 5%；120/1000 = 12%。 */
    @Test
    void shareReadout() {
        assertThat(ProbeBudget.probeShare(50, 1000)).isCloseTo(0.05, within(1e-12));
        assertThat(ProbeBudget.probeShare(120, 1000)).isCloseTo(0.12, within(1e-12));
    }

    /** 判定两态：恰 maxShare（含上）= WITHIN；超 = OVER。 */
    @Test
    void verdictTwoStates() {
        assertThat(ProbeBudget.verdict(0.05, 0.10)).isEqualTo(ProbeBudget.Verdict.WITHIN);
        assertThat(ProbeBudget.verdict(0.10, 0.10)).isEqualTo(ProbeBudget.Verdict.WITHIN);
        assertThat(ProbeBudget.verdict(0.12, 0.10)).isEqualTo(ProbeBudget.Verdict.OVER);
    }

    /** 畸形入参 fail-fast：负探测 QPS、零容量、maxShare 越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ProbeBudget.probeShare(-1, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("probeQps 不能为负");
        assertThatThrownBy(() -> ProbeBudget.probeShare(50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacityQps 不能小于 1");
        assertThatThrownBy(() -> ProbeBudget.verdict(0.5, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxShare 须在 (0,1]");
    }
}
