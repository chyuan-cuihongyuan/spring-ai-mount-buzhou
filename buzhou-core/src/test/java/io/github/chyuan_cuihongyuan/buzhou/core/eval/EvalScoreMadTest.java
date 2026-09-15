package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1700 / T2602：EvalScoreMad 纯函数直测——MAD 离散度/离群定位/哨兵与
 * 收紧档契约/自定义阈值/不可变性。
 */
class EvalScoreMadTest {

    @Test
    void emptyAndInsufficientCarrySentinel() {
        var empty = EvalScoreMad.analyze(List.of());
        assertThat(empty.count()).isZero();
        assertThat(empty.mad()).isEqualTo(-1d);
        assertThat(empty.dispersion()).isEqualTo(EvalScoreMad.Dispersion.INSUFFICIENT);
        assertThat(empty.outliers()).isEmpty();

        var two = EvalScoreMad.analyze(List.of(0.5, 0.9));
        assertThat(two.dispersion()).isEqualTo(EvalScoreMad.Dispersion.INSUFFICIENT);
        assertThat(two.mad()).isEqualTo(-1d);
    }

    @Test
    void evenAndOddMedianWithNoOutlier() {
        var odd = EvalScoreMad.analyze(List.of(1d, 2d, 3d, 4d, 5d));
        assertThat(odd.median()).isEqualTo(3d);
        assertThat(odd.mad()).isEqualTo(1d);
        assertThat(odd.dispersion()).isEqualTo(EvalScoreMad.Dispersion.SPREAD);
        assertThat(odd.outliers()).isEmpty();

        var even = EvalScoreMad.analyze(List.of(1d, 2d, 3d, 4d));
        assertThat(even.median()).isEqualTo(2.5d);
    }

    @Test
    void singleOutlierIsLocated() {
        var report = EvalScoreMad.analyze(List.of(1d, 1d, 2d, 2d, 100d));
        assertThat(report.median()).isEqualTo(2d);
        assertThat(report.mad()).isEqualTo(1d);
        assertThat(report.outliers()).containsExactly(4);
        assertThat(report.dispersion()).isEqualTo(EvalScoreMad.Dispersion.SPREAD);
    }

    @Test
    void identicalScoresAreTightWithoutOutliers() {
        var report = EvalScoreMad.analyze(List.of(5d, 5d, 5d, 5d));
        assertThat(report.median()).isEqualTo(5d);
        assertThat(report.mad()).isZero();
        assertThat(report.dispersion()).isEqualTo(EvalScoreMad.Dispersion.TIGHT);
        assertThat(report.outliers()).isEmpty();
    }

    @Test
    void tightWithDeviationFlagsDirectOutlier() {
        // MAD=0 收紧档：偏离中位数的点 z 值无定义——直接判离群
        var report = EvalScoreMad.analyze(List.of(5d, 5d, 5d, 5d, 6d));
        assertThat(report.dispersion()).isEqualTo(EvalScoreMad.Dispersion.TIGHT);
        assertThat(report.outliers()).containsExactly(4);
    }

    @Test
    void customThresholdTightensDetection() {
        // 默认 3.5 下 [1..5] 无离群；maxZ=1.0 收紧后两端点（z=1.349）入离群
        var tight = EvalScoreMad.analyze(List.of(1d, 2d, 3d, 4d, 5d), 1.0d);
        assertThat(tight.outliers()).containsExactly(0, 4);
    }

    @Test
    void nullInputIsTreatedAsEmpty() {
        var report = EvalScoreMad.analyze(null);
        assertThat(report.count()).isZero();
        assertThat(report.dispersion()).isEqualTo(EvalScoreMad.Dispersion.INSUFFICIENT);
    }

    @Test
    void reportIsImmutableAgainstCallerMutation() {
        List<Double> mutable = new ArrayList<>(List.of(1d, 2d, 3d, 4d, 5d));
        var report = EvalScoreMad.analyze(mutable);
        mutable.set(0, 999d);
        assertThat(report.scores()).containsExactly(1d, 2d, 3d, 4d, 5d);
        assertThat(report.median()).isEqualTo(3d);
    }
}
