package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 546 / T839：技能正文规模审计——规模降序+同值字典序稳定、预算超限
 * 标记、budget≤0 不判超限、null fail-fast。
 */
class SkillBodyAuditTest {

    private static Skill skill(String name, String body) {
        return new Skill(name, "desc", List.of(), body, List.of(), SkillSource.CLASSPATH);
    }

    @Test
    void rowsSortedDescWithOverBudgetFlags() {
        var report = SkillBodyAudit.analyze(List.of(
                skill("small", "abc"),
                skill("huge", "x".repeat(300)),
                skill("mid", "y".repeat(50))), 100);
        assertThat(report.skills()).isEqualTo(3);
        assertThat(report.rows().get(0).name()).isEqualTo("huge");
        assertThat(report.rows().get(0).overBudget()).isTrue();
        assertThat(report.rows().get(2).name()).isEqualTo("small");
        assertThat(report.overBudgetCount()).isEqualTo(1);
        assertThat(report.maxChars()).isEqualTo(300);
    }

    @Test
    void zeroBudgetMeansNoOverBudgetJudgement() {
        var report = SkillBodyAudit.analyze(List.of(skill("a", "x".repeat(999))), 0);
        assertThat(report.overBudgetCount()).isZero();
        assertThat(report.maxChars()).isEqualTo(999);
    }

    @Test
    void emptyListAndNullFailFast() {
        var report = SkillBodyAudit.analyze(List.of(), 100);
        assertThat(report.skills()).isZero();
        assertThat(report.avgChars()).isZero();
        assertThatThrownBy(() -> SkillBodyAudit.analyze(null, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
