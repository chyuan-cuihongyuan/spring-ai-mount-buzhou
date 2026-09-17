package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 3001 / T5004：Welford 在线方差合同——教材集手算、空态/单点
 * NaN 边界、分片 merge 等价单遍、merge 交换律与空向恒等、大偏移
 * 小抖动紧公差（Welford 稳定性的定量证据）。
 */
class WelfordAccumulatorTest {

    private static WelfordAccumulator of(double... values) {
        WelfordAccumulator acc = new WelfordAccumulator();
        for (double v : values) {
            acc.add(v);
        }
        return acc;
    }

    @Test
    void textbookSetShouldMatchHandComputation() {
        // {2,4,4,4,5,5,7,9}: mean=5、Σ(x−mean)²=32 → 样本方差 32/7、总体方差 4
        WelfordAccumulator acc = of(2, 4, 4, 4, 5, 5, 7, 9);
        assertThat(acc.count()).isEqualTo(8);
        assertThat(acc.mean()).isEqualTo(5.0);
        assertThat(acc.sampleVariance()).isCloseTo(32.0 / 7.0, within(1e-12));
        assertThat(acc.populationVariance()).isCloseTo(4.0, within(1e-12));
    }

    @Test
    void emptyAccumulatorShouldReportHonestNaN() {
        WelfordAccumulator acc = new WelfordAccumulator();
        assertThat(acc.count()).isZero();
        assertThat(acc.mean()).isNaN();
        assertThat(acc.sampleVariance()).isNaN();
        assertThat(acc.populationVariance()).isNaN();
    }

    @Test
    void singleSampleHasNoDispersion() {
        WelfordAccumulator acc = of(42);
        assertThat(acc.mean()).isEqualTo(42.0);
        assertThat(acc.populationVariance()).isZero();
        assertThat(acc.sampleVariance()).isNaN();
    }

    @Test
    void mergeShouldEqualSinglePass() {
        WelfordAccumulator whole = of(2, 4, 4, 4, 5, 5, 7, 9);
        WelfordAccumulator left = of(2, 4, 4);
        WelfordAccumulator right = of(4, 5, 5, 7, 9);
        left.merge(right);
        assertThat(left.count()).isEqualTo(whole.count());
        assertThat(left.mean()).isCloseTo(whole.mean(), within(1e-12));
        assertThat(left.sampleVariance()).isCloseTo(whole.sampleVariance(), within(1e-12));
        assertThat(left.populationVariance()).isCloseTo(whole.populationVariance(), within(1e-12));
    }

    @Test
    void mergeShouldBeCommutative() {
        WelfordAccumulator ab = of(1, 2, 3, 10);
        WelfordAccumulator ba = of(10, 3, 2, 1);
        WelfordAccumulator other = of(4, 5, 6, 100);
        ab.merge(other);
        WelfordAccumulator otherFirst = of(4, 5, 6, 100);
        otherFirst.merge(ba);
        assertThat(ab.mean()).isCloseTo(otherFirst.mean(), within(1e-12));
        assertThat(ab.sampleVariance()).isCloseTo(otherFirst.sampleVariance(), within(1e-12));
        assertThat(ab.count()).isEqualTo(otherFirst.count());
    }

    @Test
    void mergeWithEmptyShouldBeIdentityBothWays() {
        WelfordAccumulator acc = of(3, 7, 11);
        WelfordAccumulator snapshot = of(3, 7, 11);
        acc.merge(new WelfordAccumulator());
        assertThat(acc.count()).isEqualTo(snapshot.count());
        assertThat(acc.mean()).isEqualTo(snapshot.mean());
        assertThat(acc.sampleVariance()).isEqualTo(snapshot.sampleVariance());

        WelfordAccumulator empty = new WelfordAccumulator();
        empty.merge(snapshot);
        assertThat(empty.count()).isEqualTo(3);
        assertThat(empty.mean()).isEqualTo(snapshot.mean());
        assertThat(empty.sampleVariance()).isEqualTo(snapshot.sampleVariance());
    }

    @Test
    void largeOffsetSmallWiggleShouldStayExact() {
        // 1e9 基线 + {1,2,3} 抖动：方差只由抖动决定（样本方差 1、总体 2/3）
        // ——朴素 Σx² 在此量级两_BIG 数相减精度崩塌，Welford 精确
        double base = 1e9;
        WelfordAccumulator acc = of(base + 1, base + 2, base + 3);
        assertThat(acc.mean()).isCloseTo(base + 2, within(1e-6));
        assertThat(acc.sampleVariance()).isCloseTo(1.0, within(1e-9));
        assertThat(acc.populationVariance()).isCloseTo(2.0 / 3.0, within(1e-9));
    }
}
