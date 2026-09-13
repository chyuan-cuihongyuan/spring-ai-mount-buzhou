package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 821 / T1144：lint 严重度分级回归——默认映射/严重序排序/三档计数/
 * 定制规则覆盖/未知规则归 HINT。
 */
class LintSeverityGraderTest {

    private static ToolCatalogLinter.Finding f(String tool, String rule, String detail) {
        return new ToolCatalogLinter.Finding(tool, rule, detail);
    }

    @Test
    void defaultMappingAndSorting() {
        LintSeverityGrader grader = new LintSeverityGrader();
        var report = grader.grade(List.of(
                f("t1", ToolCatalogLinter.RULE_DESC, "太长"),
                f("t2", ToolCatalogLinter.RULE_DUP, "重复"),
                f("t3", ToolCatalogLinter.RULE_NAME, "命名")));

        assertThat(report.denyCount()).isEqualTo(1);
        assertThat(report.warnCount()).isEqualTo(1);
        assertThat(report.hintCount()).isEqualTo(1);
        // 严重→轻：DENY(DUP) → WARN(NAME) → HINT(DESC)
        assertThat(report.graded().get(0).finding().rule()).isEqualTo(ToolCatalogLinter.RULE_DUP);
        assertThat(report.graded().get(0).severity()).isEqualTo(LintSeverityGrader.Severity.DENY);
        assertThat(report.graded().get(1).severity()).isEqualTo(LintSeverityGrader.Severity.WARN);
        assertThat(report.graded().get(2).severity()).isEqualTo(LintSeverityGrader.Severity.HINT);
    }

    @Test
    void customRuleOverrideAndUnknownDefaultsHint() {
        LintSeverityGrader grader = new LintSeverityGrader()
                .withRule("NEW_RULE", LintSeverityGrader.Severity.DENY);
        var report = grader.grade(List.of(
                f("t", "NEW_RULE", "x"),
                f("t", "MYSTERY_RULE", "y")));

        assertThat(report.graded().get(0).finding().rule()).isEqualTo("NEW_RULE");
        assertThat(report.graded().get(1).severity()).isEqualTo(LintSeverityGrader.Severity.HINT);
        assertThat(report.denyCount()).isEqualTo(1);
    }

    @Test
    void withRuleIgnoresDirtyInputAndKeepsImmutable() {
        LintSeverityGrader base = new LintSeverityGrader();
        LintSeverityGrader same = base.withRule(null, LintSeverityGrader.Severity.DENY);
        LintSeverityGrader same2 = base.withRule("  ", LintSeverityGrader.Severity.DENY);
        LintSeverityGrader same3 = base.withRule("RULE", null);
        assertThat(same).isSameAs(base);
        assertThat(same2).isSameAs(base);
        assertThat(same3).isSameAs(base);

        // 原实例不受定制影响
        LintSeverityGrader customized = base.withRule(ToolCatalogLinter.RULE_DESC,
                LintSeverityGrader.Severity.DENY);
        var baseReport = base.grade(List.of(f("t", ToolCatalogLinter.RULE_DESC, "d")));
        var customReport = customized.grade(List.of(f("t", ToolCatalogLinter.RULE_DESC, "d")));
        assertThat(baseReport.graded().get(0).severity()).isEqualTo(LintSeverityGrader.Severity.HINT);
        assertThat(customReport.graded().get(0).severity()).isEqualTo(LintSeverityGrader.Severity.DENY);
    }

    @Test
    void tieBreakByToolThenRuleAndNullsSkipped() {
        LintSeverityGrader grader = new LintSeverityGrader();
        var report = grader.grade(java.util.Arrays.asList(
                f("b", ToolCatalogLinter.RULE_NAME, "x"),
                f("a", ToolCatalogLinter.RULE_NAME, "y"),
                null));
        assertThat(report.graded()).hasSize(2);
        assertThat(report.graded().get(0).finding().tool()).isEqualTo("a");
        assertThat(report.graded().get(1).finding().tool()).isEqualTo("b");
    }

    @Test
    void emptyFindingsEmptyReport() {
        var report = new LintSeverityGrader().grade(List.of());
        assertThat(report.graded()).isEmpty();
        assertThat(report.denyCount()).isZero();
        assertThat(report.warnCount()).isZero();
        assertThat(report.hintCount()).isZero();
    }
}
