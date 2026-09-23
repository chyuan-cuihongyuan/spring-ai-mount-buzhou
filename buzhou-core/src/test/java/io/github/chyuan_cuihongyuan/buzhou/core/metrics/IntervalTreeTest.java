package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.IntervalTree.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5022 / T6146：区间树合同——stabbing vs 线性扫圣像、
 * 边界与中点、空树、倒置 fail-fast、确定性。
 */
class IntervalTreeTest {

    private static final List<Interval> FIXTURE = List.of(
            new Interval(1, 5), new Interval(3, 8), new Interval(6, 10),
            new Interval(20, 30), new Interval(2, 2), new Interval(7, 7));

    /** 线性扫圣像。 */
    private static List<Interval> bruteForce(List<Interval> intervals, long point) {
        List<Interval> hits = new ArrayList<>();
        for (Interval interval : intervals) {
            if (interval.contains(point)) {
                hits.add(interval);
            }
        }
        hits.sort(java.util.Comparator.comparingLong(Interval::start)
                .thenComparingLong(Interval::end));
        return hits;
    }

    @Test
    void stabbingShouldMatchBruteForceOracle() {
        IntervalTree tree = new IntervalTree(FIXTURE);
        for (long point = -2; point <= 35; point++) {
            assertThat(tree.stabbing(point))
                    .as("point %s", point)
                    .isEqualTo(bruteForce(FIXTURE, point));
        }
    }

    @Test
    void randomizedIntervalsShouldMatchOracle() {
        Random random = new Random(5022L);
        List<Interval> intervals = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            int start = random.nextInt(200);
            intervals.add(new Interval(start, start + random.nextInt(30)));
        }
        IntervalTree tree = new IntervalTree(intervals);
        for (int probe = 0; probe < 200; probe++) {
            long point = random.nextInt(240) - 10;
            assertThat(tree.stabbing(point)).isEqualTo(bruteForce(intervals, point));
        }
    }

    @Test
    void emptyTreeShouldReturnEmptyStabbing() {
        IntervalTree empty = new IntervalTree(List.of());
        assertThat(empty.size()).isZero();
        assertThat(empty.stabbing(5)).isEmpty();
    }

    @Test
    void invertedIntervalShouldFailFast() {
        assertThatThrownBy(() -> new Interval(5L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IntervalTree(List.of(new Interval(9, 3))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
