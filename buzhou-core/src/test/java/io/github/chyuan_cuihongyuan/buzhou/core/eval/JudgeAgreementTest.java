package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 541 / T833：双 judge 一致率——完全一致 κ=1、机遇一致修正（po 高
 * pe 同高 → κ 低）、反转负相关、单侧排除、null fail-fast（scikit-learn
 * cohen_kappa_score 思想）。
 */
class JudgeAgreementTest {

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 1);
    }

    private static EvalRunResult run(String runId, List<EvalRunItemResult> items) {
        return new EvalRunResult(runId, "ds", Instant.EPOCH, Instant.EPOCH,
                items.size(), 0, 0, 0, items);
    }

    @Test
    void perfectAgreementGivesKappaOne() {
        var a = run("a", List.of(item("c1", "pass"), item("c2", "fail")));
        var b = run("b", List.of(item("c1", "pass"), item("c2", "fail")));
        var report = JudgeAgreement.analyze(a, b);
        assertThat(report.kappa()).isEqualTo(1.0);
        assertThat(report.strength()).isEqualTo("almost-perfect");
    }

    @Test
    void highPoWithHighPeYieldsLowKappa() {
        // 两 judge 都 90% 判绿（9 绿 1 红，红位错开）——po=0.8 但 pe≈0.82 → κ<0
        List<EvalRunItemResult> aItems = new java.util.ArrayList<>();
        List<EvalRunItemResult> bItems = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            aItems.add(item("c" + i, i == 0 ? "fail" : "pass"));
            bItems.add(item("c" + i, i == 9 ? "fail" : "pass"));
        }
        var report = JudgeAgreement.analyze(run("a", aItems), run("b", bItems));
        assertThat(report.observedAgreement()).isEqualTo(0.8);
        assertThat(report.kappa()).isLessThan(0.1); // 机遇修正后接近 0——朴素一致率虚高被揭穿
        assertThat(report.strength()).isIn("slight-or-none", "fair");
    }

    @Test
    void invertedJudgementsGiveNegativeKappa() {
        var a = run("a", List.of(item("c1", "pass"), item("c2", "fail")));
        var b = run("b", List.of(item("c1", "fail"), item("c2", "pass")));
        var report = JudgeAgreement.analyze(a, b);
        assertThat(report.kappa()).isLessThan(0.0);
        assertThat(report.strength()).isEqualTo("slight-or-none");
    }

    @Test
    void singleSidedExcludedAndNullFailFast() {
        var a = run("a", List.of(item("c1", "pass"), item("c2", "fail"), item("gone", "fail")));
        var b = run("b", List.of(item("c1", "pass"), item("c2", "fail")));
        var report = JudgeAgreement.analyze(a, b);
        assertThat(report.singleSided()).isEqualTo(1);
        assertThat(report.kappa()).isEqualTo(1.0); // pe<1——完全一致 κ=1
        assertThatThrownBy(() -> JudgeAgreement.analyze(null, b))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
