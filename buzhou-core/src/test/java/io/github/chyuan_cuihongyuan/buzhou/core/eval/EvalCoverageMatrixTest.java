package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1702 / T2606：EvalCoverageMatrix 纯函数直测——计数/漏测清单/熵契约。
 */
class EvalCoverageMatrixTest {

    @Test
    void countsAndDistinctLabels() {
        var report = EvalCoverageMatrix.build(List.of(
                Set.of("tool:read", "err:timeout"),
                Set.of("tool:read"),
                Set.of("err:timeout", "err:rate")));
        assertThat(report.itemCount()).isEqualTo(3);
        assertThat(report.distinctLabels()).isEqualTo(3);
        assertThat(report.labelCounts())
                .containsEntry("tool:read", 2)
                .containsEntry("err:timeout", 2)
                .containsEntry("err:rate", 1);
    }

    @Test
    void missingListIsSortedDifference() {
        var report = EvalCoverageMatrix.build(List.of(Set.of("a")));
        assertThat(report.missingFrom(Set.of("a", "c", "b"))).containsExactly("b", "c");
        assertThat(report.missingFrom(Set.of("a"))).isEmpty();
    }

    @Test
    void entropyContracts() {
        assertThat(EvalCoverageMatrix.build(List.of(Set.of("only"))).shannonEntropy()).isZero();
        assertThat(EvalCoverageMatrix.build(List.of()).shannonEntropy()).isZero();

        var balanced = EvalCoverageMatrix.build(List.of(Set.of("x"), Set.of("y")));
        assertThat(balanced.shannonEntropy()).isCloseTo(1d, within(1e-9));

        var skewed = EvalCoverageMatrix.build(List.of(
                Set.of("x"), Set.of("x"), Set.of("x"), Set.of("x"),
                Set.of("y"), Set.of("z"), Set.of("w")));
        var mid = EvalCoverageMatrix.build(List.of(Set.of("x"), Set.of("x"), Set.of("y")));
        assertThat(mid.shannonEntropy()).isGreaterThan(skewed.shannonEntropy());
        assertThat(skewed.shannonEntropy()).isBetween(0d, 1d);
    }

    @Test
    void nullAndEmptyItemSetsAreCounted() {
        var report = EvalCoverageMatrix.build(null);
        assertThat(report.itemCount()).isZero();
        var unlabeled = EvalCoverageMatrix.build(java.util.List.of(Set.of(), Set.of()));
        assertThat(unlabeled.itemCount()).isEqualTo(2);
        assertThat(unlabeled.distinctLabels()).isZero();
        assertThat(unlabeled.shannonEntropy()).isZero();
    }
}
