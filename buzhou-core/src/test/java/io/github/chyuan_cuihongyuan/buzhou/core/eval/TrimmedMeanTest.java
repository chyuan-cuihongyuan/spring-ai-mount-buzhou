package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1852 / T2906：截尾均值——离群值剔除、截空哨兵、零截退化。 */
class TrimmedMeanTest {

    /** 离群值剔除：30s 卡顿不污染中枢。 */
    @Test
    void shouldTrimOutliersFromBothEnds() {
        List<Double> latencies =
                List.of(100d, 102d, 98d, 101d, 99d, 30_000d);
        // n=6，f=1/6≈0.167 → 截 ⌊1⌋=1 头 1 尾：去 98 与 30000 → 均值 (99+100+101+102)/4
        assertThat(TrimmedMean.mean(latencies, 1.0 / 6)).isEqualTo(100.5d);
    }

    /** 零截退化算术均；对称小样本截尾。 */
    @Test
    void zeroTrimDegradesToArithmeticMean() {
        assertThat(TrimmedMean.mean(List.of(1d, 2d, 3d), 0d)).isEqualTo(2.0d);
        // n=4, f=0.25 → 截 1 头 1 尾：去 1 与 4 → (2+3)/2
        assertThat(TrimmedMean.mean(List.of(1d, 2d, 3d, 4d), 0.25d)).isEqualTo(2.5d);
    }

    /** 截空哨兵：仅空表/null（f<0.5 时数学上截不空——防御分支不可达）。 */
    @Test
    void fullyTrimmedYieldsSentinel() {
        // n=1, f=0.4 → floor(0.4)=0 不截——f<0.5 保证 from<to 恒成立
        assertThat(TrimmedMean.mean(List.of(1d), 0.4d)).isEqualTo(1.0d);
        assertThat(TrimmedMean.mean(List.of(), 0.1d)).isEqualTo(-1d);
        assertThat(TrimmedMean.mean(null, 0.1d)).isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：截尾过半、null/NaN 样本。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TrimmedMean.mean(List.of(1d), 0.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[0,0.5)");
        assertThatThrownBy(() -> TrimmedMean.mean(List.of(1d), -0.1d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrimmedMean.mean(
                java.util.Arrays.asList(1d, null), 0.1d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrimmedMean.mean(List.of(Double.NaN), 0.1d))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
