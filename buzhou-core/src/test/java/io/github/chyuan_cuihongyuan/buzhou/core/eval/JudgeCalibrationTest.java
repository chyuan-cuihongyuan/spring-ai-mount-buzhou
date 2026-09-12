package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 516 / T783–T784：judge 校准——四率数学（TP/TN/FP/FN）、完全一致
 * agreement 1、单侧排除、分母 0 null 诚实空值、null fail-fast。
 */
class JudgeCalibrationTest {

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 1);
    }

    private static EvalRunResult run(String runId, List<EvalRunItemResult> items) {
        return new EvalRunResult(runId, "ds", Instant.EPOCH, Instant.EPOCH,
                items.size(), 0, 0, 0, items);
    }

    @Test
    void perfectAgreementScoresFull() {
        var golden = run("golden", List.of(item("c1", "pass"), item("c2", "fail")));
        var judged = run("judge", List.of(item("c1", "pass"), item("c2", "fail")));
        var report = JudgeCalibration.calibrate(golden, judged);
        assertThat(report.tp()).isEqualTo(1);
        assertThat(report.tn()).isEqualTo(1);
        assertThat(report.fp()).isZero();
        assertThat(report.fn()).isZero();
        assertThat(report.agreement()).isEqualTo(1.0);
        assertThat(report.precision()).isEqualTo(1.0);
        assertThat(report.recall()).isEqualTo(1.0);
        assertThat(report.f1()).isEqualTo(1.0);
    }

    @Test
    void confusionMatrixMathWithOneFpAndOneFn() {
        // 金标准：c1 绿 c2 红 c3 绿 c4 红
        var golden = run("golden", List.of(
                item("c1", "pass"), item("c2", "fail"),
                item("c3", "pass"), item("c4", "fail")));
        // judge：c1 判红（FP）c2 判红（TP）c3 判绿（TN）c4 判绿（FN）
        var judged = run("judge", List.of(
                item("c1", "fail"), item("c2", "fail"),
                item("c3", "pass"), item("c4", "pass")));
        var report = JudgeCalibration.calibrate(golden, judged);
        assertThat(report.tp()).isEqualTo(1);
        assertThat(report.tn()).isEqualTo(1);
        assertThat(report.fp()).isEqualTo(1);
        assertThat(report.fn()).isEqualTo(1);
        assertThat(report.precision()).isEqualTo(0.5);
        assertThat(report.recall()).isEqualTo(0.5);
        assertThat(report.f1()).isEqualTo(0.5);
        assertThat(report.agreement()).isEqualTo(0.5);
        assertThat(report.singleSided()).isZero();
    }

    @Test
    void singleSidedExcludedFromConfusion() {
        var golden = run("golden", List.of(item("c1", "pass"), item("gone", "fail")));
        var judged = run("judge", List.of(item("c1", "pass"), item("new", "pass")));
        var report = JudgeCalibration.calibrate(golden, judged);
        assertThat(report.singleSided()).isEqualTo(2);
        assertThat(report.tp() + report.tn() + report.fp() + report.fn()).isEqualTo(1);
        assertThat(report.agreement()).isEqualTo(1.0);
    }

    @Test
    void zeroDenominatorMetricsAreNullNotZero() {
        // 全绿金标准：judge 全判绿 → TP+FN=0 → recall null（不是 0）
        var golden = run("golden", List.of(item("c1", "pass"), item("c2", "pass")));
        var judged = run("judge", List.of(item("c1", "pass"), item("c2", "pass")));
        var report = JudgeCalibration.calibrate(golden, judged);
        assertThat(report.recall()).isNull();
        assertThat(report.f1()).isNull();
        assertThat(report.precision()).isNull(); // TP+FP=0 → precision null（诚实空值）
        assertThat(report.agreement()).isEqualTo(1.0);
    }

    @Test
    void nullRunsFailFast() {
        var empty = run("r", List.of());
        assertThatThrownBy(() -> JudgeCalibration.calibrate(null, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
