package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2056 / T3214：五数概括合同——五点手算（R-7 线性）、IQR 围栏、
 * 离群判定、单点退化、乱序入参稳定、畸形 fail-fast。
 */
class FiveNumberSummaryTest {

    @Test
    void fiveNumbersShouldMatchHandComputation() {
        // 1..9：min 1 / Q1 3 / 中位 5 / Q3 7 / max 9（恰线性无需插值）
        FiveNumberSummary.Summary s = FiveNumberSummary.of(
                new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertThat(s.min()).isEqualTo(1.0d);
        assertThat(s.q1()).isEqualTo(3.0d);
        assertThat(s.median()).isEqualTo(5.0d);
        assertThat(s.q3()).isEqualTo(7.0d);
        assertThat(s.max()).isEqualTo(9.0d);
        assertThat(s.iqr()).isEqualTo(4.0d);
    }

    @Test
    void evenSampleMedianShouldInterpolate() {
        // 4 样本：中位 (2+3)/2=2.5，Q1 位置 0.75 → 1.25+... R-7 手算
        FiveNumberSummary.Summary s = FiveNumberSummary.of(new double[]{1, 2, 3, 4});
        assertThat(s.median()).isCloseTo(2.5d, within(1e-12));
        assertThat(s.q1()).isCloseTo(1.75d, within(1e-12)); // h=0.75：1+0.75×(2−1)
        assertThat(s.q3()).isCloseTo(3.25d, within(1e-12));
    }

    @Test
    void fencesShouldFollowIqrConvention() {
        // 1..9：IQR=4，围栏 [3−6, 7+6] = [−3, 13]
        FiveNumberSummary.Summary s = FiveNumberSummary.of(
                new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertThat(s.lowerFence()).isCloseTo(-3.0d, within(1e-12));
        assertThat(s.upperFence()).isCloseTo(13.0d, within(1e-12));
    }

    @Test
    void outliersShouldBeDetectedOutsideFences() {
        // 典型分布 + 一个极端高值
        FiveNumberSummary.Summary s = FiveNumberSummary.of(
                new double[]{10, 11, 12, 13, 14, 15, 16, 17, 100});
        assertThat(s.isOutlier(100)).isTrue();   // 围栏外离群
        assertThat(s.isOutlier(13)).isFalse();   // 箱内
        assertThat(s.isOutlier(s.upperFence())).isFalse(); // 恰界内（< 上界语义按外才离）
    }

    @Test
    void singleSampleShouldDegenerateToConstant() {
        FiveNumberSummary.Summary s = FiveNumberSummary.of(new double[]{42});
        assertThat(s.min()).isEqualTo(s.max()).isEqualTo(42.0d);
        assertThat(s.iqr()).isZero();
        assertThat(s.lowerFence()).isEqualTo(42.0d); // IQR=0 围栏=箱体
    }

    @Test
    void inputOrderShouldNotMatter() {
        FiveNumberSummary.Summary a = FiveNumberSummary.of(
                new double[]{5, 1, 9, 3, 7, 2, 8, 4, 6});
        FiveNumberSummary.Summary b = FiveNumberSummary.of(
                new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        assertThat(a.median()).isEqualTo(b.median());
        assertThat(a.q1()).isEqualTo(b.q1());
        assertThat(a.q3()).isEqualTo(b.q3());
    }

    @Test
    void customFenceFactorShouldWidenFences() {
        double[] samples = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        FiveNumberSummary.Summary inner = FiveNumberSummary.of(samples, 1.5d);
        FiveNumberSummary.Summary outer = FiveNumberSummary.of(samples, 3.0d);
        assertThat(outer.upperFence()).isGreaterThan(inner.upperFence());
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> FiveNumberSummary.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FiveNumberSummary.of(new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FiveNumberSummary.of(new double[]{1, Double.NaN}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FiveNumberSummary.of(new double[]{1}, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
