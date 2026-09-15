package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1843 / T2888：依赖图三病——环/缺失/孤儿分诊。 */
class SkillDependencyAuditTest {

    /** 健康 DAG：无环、无缺失、无孤儿。 */
    @Test
    void healthyDagReadsClean() {
        SkillDependencyAudit.Audit audit = SkillDependencyAudit.audit(
                Set.of("a", "b", "c"),
                List.of(new SkillDependencyAudit.Edge("a", "b"),
                        new SkillDependencyAudit.Edge("b", "c")));
        assertThat(audit.hasCycle()).isFalse();
        assertThat(audit.exampleCycle()).isEmpty();
        assertThat(audit.missingDependencies()).isZero();
        assertThat(audit.orphanSkills()).isZero();
        assertThat(audit.skills()).isEqualTo(3);
        assertThat(audit.edges()).isEqualTo(2);
    }

    /** 环检测：a→b→c→a 回边，环路径首尾同点。 */
    @Test
    void shouldDetectCycleWithPath() {
        SkillDependencyAudit.Audit audit = SkillDependencyAudit.audit(
                Set.of("a", "b", "c"),
                List.of(new SkillDependencyAudit.Edge("a", "b"),
                        new SkillDependencyAudit.Edge("b", "c"),
                        new SkillDependencyAudit.Edge("c", "a")));
        assertThat(audit.hasCycle()).isTrue();
        assertThat(audit.exampleCycle()).isNotEmpty();
        assertThat(audit.exampleCycle().get(0))
                .isEqualTo(audit.exampleCycle().get(audit.exampleCycle().size() - 1));
        assertThat(audit.exampleCycle()).contains("a", "b", "c");
    }

    /** 缺失依赖与孤儿分账：边指向未声明技能、声明技能无边关联。 */
    @Test
    void shouldCountMissingAndOrphans() {
        SkillDependencyAudit.Audit audit = SkillDependencyAudit.audit(
                Set.of("a", "b", "ghost-is-not-here"),
                List.of(new SkillDependencyAudit.Edge("a", "b"),
                        new SkillDependencyAudit.Edge("a", "missing-target")));
        assertThat(audit.missingDependencies()).isEqualTo(1);
        assertThat(audit.orphanSkills()).isEqualTo(1); // ghost-is-not-here
        assertThat(audit.hasCycle()).isFalse();
    }

    /** 空输入哨兵：无技能无边零账。 */
    @Test
    void emptyInputsYieldZeroAudit() {
        SkillDependencyAudit.Audit audit = SkillDependencyAudit.audit(null, null);
        assertThat(audit.skills()).isZero();
        assertThat(audit.edges()).isZero();
        assertThat(audit.hasCycle()).isFalse();
        assertThat(audit.orphanSkills()).isZero();
    }

    /** 畸形入参 fail-fast：空白边、自指边、超保险丝。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new SkillDependencyAudit.Edge(" ", "b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("两端须非空白");
        assertThatThrownBy(() -> new SkillDependencyAudit.Edge("a", "a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能自指");
    }
}
