package io.github.chyuan_cuihongyuan.buzhou.skill.classpath;

import io.github.chyuan_cuihongyuan.buzhou.skill.Skill;

import java.util.Map;

/**
 * 一个 classpath 内置 Skill 的扫描产物：解析后的 {@link Skill}（含正文与资源元数据）+
 * 资源相对路径 → 文本内容的映射（资源按需读取时直返，不落盘）。
 */
public record ClasspathSkillEntry(Skill skill, Map<String, String> resourceContents) {

    public ClasspathSkillEntry {
        resourceContents = resourceContents == null ? Map.of() : Map.copyOf(resourceContents);
    }
}
