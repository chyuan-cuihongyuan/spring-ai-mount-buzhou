package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link SkillFrontmatterParser} 受限 YAML 子集解析测试（spec 04 frontmatter）。 */
class SkillFrontmatterParserTest {

    @Test
    void parsesNameDescriptionAndAllowedTools() {
        String md = """
                ---
                name: code-review
                description: 代码评审清单与严重度分级标准
                allowed-tools: read_file, read_range, run_command
                ---

                # Code Review Skill
                正文内容
                """;
        ParsedSkillMd parsed = ParsedSkillMd.parse(md);

        assertThat(parsed.frontmatter().name()).isEqualTo("code-review");
        assertThat(parsed.frontmatter().description()).isEqualTo("代码评审清单与严重度分级标准");
        assertThat(parsed.frontmatter().allowedTools())
                .containsExactly("read_file", "read_range", "run_command");
        assertThat(parsed.body()).startsWith("# Code Review Skill").contains("正文内容");
    }

    @Test
    void allowedToolsUnderscoreKeyNormalized() {
        // 下划线键名归一化为连字符
        String md = """
                ---
                name: s1
                allowed_tools: a, b
                ---
                body
                """;
        assertThat(ParsedSkillMd.parse(md).frontmatter().allowedTools()).containsExactly("a", "b");
    }

    @Test
    void quotedValuesUnwrapped() {
        String md = """
                ---
                name: "quoted-name"
                description: '带引号的描述'
                ---
                b
                """;
        ParsedSkillMd parsed = ParsedSkillMd.parse(md);
        assertThat(parsed.frontmatter().name()).isEqualTo("quoted-name");
        assertThat(parsed.frontmatter().description()).isEqualTo("带引号的描述");
    }

    @Test
    void noFrontmatterYieldsBodyOnly() {
        ParsedSkillMd parsed = ParsedSkillMd.parse("# 仅正文\n无 frontmatter");
        assertThat(parsed.frontmatter().name()).isEmpty();
        assertThat(parsed.body()).contains("仅正文");
    }

    @Test
    void unknownKeysIgnoredForForwardCompat() {
        String md = """
                ---
                name: s
                future-field: value
                ---
                b
                """;
        ParsedSkillMd parsed = ParsedSkillMd.parse(md);
        assertThat(parsed.frontmatter().name()).isEqualTo("s");
    }

    @Test
    void emptyAllowedToolsDefaultsToEmpty() {
        String md = """
                ---
                name: s
                ---
                b
                """;
        assertThat(ParsedSkillMd.parse(md).frontmatter().allowedTools()).isEmpty();
    }
}
