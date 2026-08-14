package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import io.github.chyuan_cuihongyuan.buzhou.skill.Skill;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link ClasspathSkillScanner} 扫描 META-INF/skills 测试（spec 04：jar 内置引依赖即得）。 */
class ClasspathSkillScannerTest {

    @Test
    void scansTestClasspathSkills() {
        Map<String, ClasspathSkillEntry> skills = new ClasspathSkillScanner().scan();

        assertThat(skills).containsKeys("code-review", "sql-tuning");
    }

    @Test
    void parsesBodyAndResources() {
        Map<String, ClasspathSkillEntry> skills = new ClasspathSkillScanner().scan();
        ClasspathSkillEntry codeReview = skills.get("code-review");

        assertThat(codeReview).isNotNull();
        Skill skill = codeReview.skill();
        assertThat(skill.source()).isEqualTo(SkillSource.CLASSPATH);
        assertThat(skill.description()).isEqualTo("代码评审清单与严重度分级标准");
        assertThat(skill.allowedTools()).containsExactly("read_file", "read_range", "run_command");
        assertThat(skill.body()).contains("# Code Review Skill", "严重度分级");

        // 资源元数据 + 内容（行尾归一化比对：Windows checkout autocrlf 下资源为 CRLF，Java 文本块恒 LF）
        assertThat(skill.resources()).anyMatch(r -> r.relativePath().equals("checklists/security.md"));
        String checklist = codeReview.resourceContents().get("checklists/security.md");
        assertThat(checklist).isNotNull();
        assertThat(checklist.replace("\r\n", "\n")).isEqualTo("""
                # 安全核查清单

                - [ ] 输入是否经校验/转义（防注入）
                - [ ] 鉴权是否覆盖该路径
                - [ ] 敏感信息是否避免落日志
                - [ ] 依赖是否有已知 CVE
                """);
    }

    @Test
    void skillWithoutResourcesHasEmptyResourceList() {
        Map<String, ClasspathSkillEntry> skills = new ClasspathSkillScanner().scan();
        ClasspathSkillEntry sqlTuning = skills.get("sql-tuning");

        assertThat(sqlTuning).isNotNull();
        assertThat(sqlTuning.skill().resources()).isEmpty();
        assertThat(sqlTuning.resourceContents()).isEmpty();
    }
}
