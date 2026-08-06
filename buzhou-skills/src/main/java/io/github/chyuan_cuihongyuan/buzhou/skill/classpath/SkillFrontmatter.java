package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import java.util.List;

/** SKILL.md 的 YAML frontmatter（对齐 Claude Code：name/description/allowed-tools）。 */
public record SkillFrontmatter(String name, String description, List<String> allowedTools) {

    /** 空占位（无 frontmatter 的纯正文）。 */
    public static final SkillFrontmatter EMPTY = new SkillFrontmatter("", "", List.of());

    public SkillFrontmatter {
        allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    }
}
