package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1801 / T2804：PSI some/full 失速账目——部分失速与全失速分开计。 */
class SpillPressureStallTest {

    /** 账目正确：some 计一切非零失速窗，full 只计全会话失速窗；峰值窗取失速数最大者。 */
    @Test
    void shouldAccountSomeAndFullSeparately() {
        SpillPressureStall.PsiReport report = SpillPressureStall.analyze(List.of(
                new SpillPressureStall.StallSample(4, 0),
                new SpillPressureStall.StallSample(4, 1),
                new SpillPressureStall.StallSample(4, 4),
                new SpillPressureStall.StallSample(2, 2)));
        assertThat(report.windows()).isEqualTo(4);
        assertThat(report.someWindows()).isEqualTo(3);
        assertThat(report.fullWindows()).isEqualTo(2);
        assertThat(report.somePct()).isEqualTo(0.75d);
        assertThat(report.fullPct()).isEqualTo(0.5d);
        assertThat(report.worstStalled()).isEqualTo(4);
        assertThat(report.worstActive()).isEqualTo(4);
        assertThat(report.worstStallRatio()).isEqualTo(1.0d);
    }

    /** 空闲窗（active=0）只进分母：既不算 some 也不算 full。 */
    @Test
    void idleWindowCountsOnlyInDenominator() {
        SpillPressureStall.PsiReport report = SpillPressureStall.analyze(List.of(
                new SpillPressureStall.StallSample(0, 0),
                new SpillPressureStall.StallSample(0, 0)));
        assertThat(report.windows()).isEqualTo(2);
        assertThat(report.someWindows()).isZero();
        assertThat(report.fullWindows()).isZero();
        assertThat(report.somePct()).isZero();
        assertThat(report.fullPct()).isZero();
        assertThat(report.worstStallRatio()).isEqualTo(-1d);
    }

    /** 空表与 null 同口径：全读数 -1 哨兵、分母 0。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (SpillPressureStall.PsiReport report : List.of(
                SpillPressureStall.analyze(List.of()),
                SpillPressureStall.analyze(null))) {
            assertThat(report.windows()).isZero();
            assertThat(report.somePct()).isEqualTo(-1d);
            assertThat(report.fullPct()).isEqualTo(-1d);
            assertThat(report.worstStallRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形样本 fail-fast：负数与 stalled>active 都在构造期拒绝。 */
    @Test
    void malformedSampleFailsFast() {
        assertThatThrownBy(() -> new SpillPressureStall.StallSample(2, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ stalled ≤ active");
        assertThatThrownBy(() -> new SpillPressureStall.StallSample(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 峰值并列时取失速面更宽的窗（同失速数下活跃更多的窗信息量更大）。 */
    @Test
    void worstWindowPrefersWiderStallFaceOnTie() {
        SpillPressureStall.PsiReport report = SpillPressureStall.analyze(List.of(
                new SpillPressureStall.StallSample(3, 2),
                new SpillPressureStall.StallSample(10, 2)));
        assertThat(report.worstStalled()).isEqualTo(2);
        assertThat(report.worstActive()).isEqualTo(10);
        assertThat(report.worstStallRatio()).isEqualTo(0.2d);
    }
}
