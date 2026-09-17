package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.SweepLineIntervals.Interval;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.SweepLineIntervals.SweepResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3013 / T5028：扫线合同——重叠峰值手算、相接不算并发（半开
 * 语义）、三层嵌套峰 3、全不相交峰 1、空集哨兵、零宽贡献零、
 * 破缺区间 fail-fast、乱序输入序不变、同峰取最早首发点。
 */
class SweepLineIntervalsTest {

    @Test
    void overlappingIntervalsShouldPeakAtTwo() {
        // [0,10)+[5,15)+[12,20)：峰 2 首发于 5
        SweepResult result = SweepLineIntervals.sweep(List.of(
                new Interval(0, 10), new Interval(5, 15), new Interval(12, 20)));
        assertThat(result.maxConcurrent()).isEqualTo(2);
        assertThat(result.atPoint()).isEqualTo(5);
        assertThat(result.intervals()).isEqualTo(3);
    }

    @Test
    void touchingIntervalsShouldNotCountAsConcurrent() {
        // [0,5)+[5,10)：前尾=后头，−1 先处理——恒 1（闭区间口径会误报 2）
        SweepResult result = SweepLineIntervals.sweep(List.of(
                new Interval(0, 5), new Interval(5, 10)));
        assertThat(result.maxConcurrent()).isEqualTo(1);
    }

    @Test
    void nestedIntervalsShouldPeakAtThree() {
        SweepResult result = SweepLineIntervals.sweep(List.of(
                new Interval(0, 10), new Interval(2, 8), new Interval(4, 6)));
        assertThat(result.maxConcurrent()).isEqualTo(3);
        assertThat(result.atPoint()).isEqualTo(4);
    }

    @Test
    void disjointIntervalsShouldPeakAtOne() {
        SweepResult result = SweepLineIntervals.sweep(List.of(
                new Interval(10, 20), new Interval(0, 5), new Interval(30, 40)));
        assertThat(result.maxConcurrent()).isEqualTo(1);
        assertThat(result.atPoint()).isEqualTo(0);
    }

    @Test
    void emptyInputShouldBeHonest() {
        SweepResult result = SweepLineIntervals.sweep(List.of());
        assertThat(result.maxConcurrent()).isZero();
        assertThat(result.atPoint()).isEqualTo(SweepLineIntervals.NO_PEAK_POINT);
        assertThat(result.intervals()).isZero();
    }

    @Test
    void zeroWidthIntervalContributesNothing() {
        SweepResult result = SweepLineIntervals.sweep(List.of(new Interval(5, 5)));
        assertThat(result.maxConcurrent()).isZero();
        assertThat(result.intervals()).isEqualTo(1);
    }

    @Test
    void startAfterEndShouldFailFast() {
        assertThatThrownBy(() -> SweepLineIntervals.sweep(List.of(new Interval(9, 3))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SweepLineIntervals.sweep(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputOrderShouldNotMatter() {
        List<Interval> ordered = List.of(
                new Interval(0, 10), new Interval(2, 8), new Interval(4, 6));
        List<Interval> shuffled = new ArrayList<>(ordered);
        Collections.shuffle(shuffled, new java.util.Random(42));
        assertThat(SweepLineIntervals.sweep(shuffled)).isEqualTo(SweepLineIntervals.sweep(ordered));
    }

    @Test
    void tiePeakShouldReportEarliestPoint() {
        // 两个独立峰 2（@1 与 @11）——首发点取最早 1
        SweepResult result = SweepLineIntervals.sweep(List.of(
                new Interval(0, 2), new Interval(1, 3),
                new Interval(10, 12), new Interval(11, 13)));
        assertThat(result.maxConcurrent()).isEqualTo(2);
        assertThat(result.atPoint()).isEqualTo(1);
    }
}
