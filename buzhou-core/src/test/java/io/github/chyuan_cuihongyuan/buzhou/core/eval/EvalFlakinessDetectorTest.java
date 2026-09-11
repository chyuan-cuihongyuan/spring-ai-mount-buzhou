package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 513 / T777–T778：A/A 抖动检测——全同零抖动、单项翻转判抖动
 * （fail/error 同红不互抖）、pass↔error 折红翻转、单侧项漂移不进分母、
 * flakyRate 数学、null fail-fast。
 */
class EvalFlakinessDetectorTest {

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 1);
    }

    private static EvalRunResult run(String runId, List<EvalRunItemResult> items) {
        return new EvalRunResult(runId, "ds", Instant.EPOCH, Instant.EPOCH,
                items.size(), 0, 0, 0, items);
    }

    @Test
    void identicalRunsHaveZeroFlakiness() {
        var a = run("r1", List.of(item("c1", "pass"), item("c2", "fail"), item("c3", "error")));
        var b = run("r2", List.of(item("c1", "pass"), item("c2", "fail"), item("c3", "error")));
        var report = EvalFlakinessDetector.analyze(a, b);
        assertThat(report.compared()).isEqualTo(3);
        assertThat(report.flakyItems()).isEmpty();
        assertThat(report.flakyRate()).isZero();
        assertThat(report.driftItems()).isEmpty();
    }

    @Test
    void verdictFlipIsFlakyRegardlessOfDirection() {
        // c1: pass→fail（REGRESSION 方向）与 c2: fail→pass（FIX 方向）——A/A 无方向都算抖动
        var a = run("r1", List.of(item("c1", "pass"), item("c2", "fail")));
        var b = run("r2", List.of(item("c1", "fail"), item("c2", "pass")));
        var report = EvalFlakinessDetector.analyze(a, b);
        assertThat(report.compared()).isEqualTo(2);
        assertThat(report.flakyItems()).hasSize(2);
        assertThat(report.flakyRate()).isEqualTo(1.0);
        assertThat(report.flakyItems().get(0).statusA()).isEqualTo("pass");
        assertThat(report.flakyItems().get(0).statusB()).isEqualTo("fail");
    }

    @Test
    void errorCountsAsRedAndFlipsWithPassButNotWithFail() {
        var a = run("r1", List.of(item("c1", "pass"), item("c2", "fail")));
        var b = run("r2", List.of(item("c1", "error"), item("c2", "error")));
        var report = EvalFlakinessDetector.analyze(a, b);
        // c1: pass↔error = 翻转；c2: fail↔error 同红 = 不抖
        assertThat(report.flakyItems()).hasSize(1);
        assertThat(report.flakyItems().getFirst().itemId()).isEqualTo("c1");
        assertThat(report.flakyRate()).isEqualTo(0.5);
    }

    @Test
    void singleSidedItemsAreDriftNotFlakiness() {
        var a = run("r1", List.of(item("c1", "pass"), item("gone", "pass")));
        var b = run("r2", List.of(item("c1", "pass"), item("new", "fail")));
        var report = EvalFlakinessDetector.analyze(a, b);
        assertThat(report.compared()).isEqualTo(1); // 只有 c1 双侧
        assertThat(report.driftItems()).containsExactlyInAnyOrder("gone", "new");
        assertThat(report.flakyItems()).isEmpty();
        assertThat(report.flakyRate()).isZero();
    }

    @Test
    void emptyRunsAndNullFailFast() {
        var empty = run("r1", List.of());
        var report = EvalFlakinessDetector.analyze(empty, empty);
        assertThat(report.compared()).isZero();
        assertThat(report.flakyRate()).isZero(); // 0 项约定 0
        assertThatThrownBy(() -> EvalFlakinessDetector.analyze(null, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
