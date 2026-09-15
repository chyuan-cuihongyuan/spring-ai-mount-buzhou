package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1856 / T2914：利特尔法则——隐含并发、互证、容差。 */
class LittlesLawAuditTest {

    /** 隐含并发：λ=10/s × W=200ms → L=2（单位换算内置）。 */
    @Test
    void shouldComputeImpliedConcurrency() {
        assertThat(LittlesLawAudit.impliedConcurrency(10d, 200d)).isEqualTo(2.0d);
        assertThat(LittlesLawAudit.impliedConcurrency(0d, 500d)).isZero();
        assertThat(LittlesLawAudit.impliedConcurrency(5d, 0d)).isZero();
    }

    /** 互证：实测 2.05 vs 隐含 2.0 在 5% 容差内一致；实测 4 严重分歧。 */
    @Test
    void shouldAuditConsistencyWithinTolerance() {
        assertThat(LittlesLawAudit.consistency(2.05d, 10d, 200d, 0.05d))
                .isEqualTo(LittlesLawAudit.Consistency.CONSISTENT);
        assertThat(LittlesLawAudit.consistency(4.0d, 10d, 200d, 0.05d))
                .isEqualTo(LittlesLawAudit.Consistency.DIVERGENT);
    }

    /** 零基线退化：λ=0 隐含 0，实测 0.5 在容差 1×max(0,1)=1 内一致。 */
    @Test
    void zeroBaselineDegradesGracefully() {
        assertThat(LittlesLawAudit.consistency(0.5d, 0d, 0d, 1.0d))
                .isEqualTo(LittlesLawAudit.Consistency.CONSISTENT);
        assertThat(LittlesLawAudit.consistency(2.5d, 0d, 0d, 1.0d))
                .isEqualTo(LittlesLawAudit.Consistency.DIVERGENT);
    }

    /** 畸形入参 fail-fast：负值/NaN。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> LittlesLawAudit.impliedConcurrency(-1d, 100d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("须 ≥ 0 非 NaN");
        assertThatThrownBy(() -> LittlesLawAudit.impliedConcurrency(1d, -1d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LittlesLawAudit.consistency(
                -1d, 1d, 1d, 0.1d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LittlesLawAudit.consistency(
                1d, 1d, 1d, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
