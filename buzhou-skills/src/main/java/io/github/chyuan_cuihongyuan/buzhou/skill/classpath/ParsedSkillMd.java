package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import java.util.List;

/** {@code SKILL.md} 解析结果：frontmatter 元数据 + Markdown 正文。 */
public record ParsedSkillMd(SkillFrontmatter frontmatter, String body) {

    /** 解析 SKILL.md 文本：切分 YAML frontmatter（{@code ---} 包裹）与正文。 */
    public static ParsedSkillMd parse(String content) {
        return SkillFrontmatterParser.parse(content);
    }

    /** frontmatter 占位（无 frontmatter 的纯正文）。 */
    public static ParsedSkillMd bodyOnly(String body, String fallbackName) {
        return new ParsedSkillMd(new SkillFrontmatter(fallbackName, "", List.of()), body);
    }
}
