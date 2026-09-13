package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-661 / spec 908：k 次 run 稳定性矩阵——全一致稳定、翻转抖动、漂移项不入
 * 分母、红绿从严（error=红）、runId 重复/k&lt;2 fail-fast、两 run 版零回归。
 */
class EvalKStabilityTest {

    private static EvalRunResult run(String runId, EvalRunItemResult... items) {
        return new EvalRunResult(runId, "ds", Instant.EPOCH, Instant.EPOCH,
                items.length, (int) List.of(items).stream()
                        .filter(i -> i.status().equals("pass")).count(),
                0, 0, List.of(items), null);
    }

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 0);
    }

    @Test
    void allConsistentVerdictsAreStable() {
        EvalFlakinessDetector.KStabilityReport report = EvalFlakinessDetector.analyzeK(List.of(
                run("r1", item("a", "pass"), item("b", "pass")),
                run("r2", item("a", "pass"), item("b", "pass")),
                run("r3", item("a", "pass"), item("b", "pass"))));

        assertThat(report.runCount()).isEqualTo(3);
        assertThat(report.compared()).isEqualTo(2);
        assertThat(report.stableItems()).isEqualTo(2);
        assertThat(report.flakyItems()).isZero();
        assertThat(report.flakyRate()).isZero();
        assertThat(report.verdicts()).allSatisfy(EvalFlakinessDetector.KItemVerdict::stable);
    }

    @Test
    void flipAcrossRunsIsFlaky() {
        // 项 a：pass/pass/fail——翻转；项 b：fail/fail/fail——稳定红
        EvalFlakinessDetector.KStabilityReport report = EvalFlakinessDetector.analyzeK(List.of(
                run("r1", item("a", "pass"), item("b", "fail")),
                run("r2", item("a", "pass"), item("b", "fail")),
                run("r3", item("a", "fail"), item("b", "fail"))));

        assertThat(report.compared()).isEqualTo(2);
        assertThat(report.stableItems()).isEqualTo(1);
        assertThat(report.flakyItems()).isEqualTo(1);
        assertThat(report.flakyRate()).isEqualTo(0.5);
        EvalFlakinessDetector.KItemVerdict verdictA = report.verdicts().stream()
                .filter(v -> v.itemId().equals("a")).findFirst().orElseThrow();
        assertThat(verdictA.stable()).isFalse();
        assertThat(verdictA.statuses()).containsExactly("pass", "pass", "fail");
    }

    @Test
    void errorIsRedStrictlyAndDriftExcluded() {
        EvalFlakinessDetector.KStabilityReport report = EvalFlakinessDetector.analyzeK(List.of(
                run("r1", item("a", "error"), item("only-r1", "pass")),
                run("r2", item("a", "error"), item("only-r2", "pass"))));

        // error=红（从严）：a 稳定；单侧项漂移不入分母
        assertThat(report.compared()).isEqualTo(1);
        assertThat(report.stableItems()).isEqualTo(1);
        assertThat(report.verdicts()).allSatisfy(v -> v.itemId().equals("a"));
    }

    @Test
    void argsValidated() {
        assertThatThrownBy(() -> EvalFlakinessDetector.analyzeK(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalFlakinessDetector.analyzeK(List.of(
                run("only", item("a", "pass")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalFlakinessDetector.analyzeK(List.of(
                run("dup", item("a", "pass")),
                run("dup", item("a", "pass")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoRunPathStillWorks() {
        // 既有两 run analyze 零回归（同一数据两法口径一致）
        EvalRunResult a = run("r1", item("a", "pass"), item("b", "fail"));
        EvalRunResult b = run("r2", item("a", "fail"), item("b", "fail"));
        EvalFlakinessDetector.FlakinessReport pair = EvalFlakinessDetector.analyze(a, b);
        EvalFlakinessDetector.KStabilityReport kReport = EvalFlakinessDetector.analyzeK(List.of(a, b));

        assertThat(pair.compared()).isEqualTo(2);
        assertThat(pair.flakyItems()).hasSize(1);
        assertThat(kReport.flakyItems()).isEqualTo(1);
        assertThat(kReport.flakyRate()).isEqualTo(pair.flakyRate());
    }
}
