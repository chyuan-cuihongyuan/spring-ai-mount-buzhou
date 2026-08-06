package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSource;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;

/** 管理视图：DB 与内置 Skill 合并展示的单条摘要（标注来源/状态/覆盖关系）。 */
public record SkillSummary(String name, String description, SkillSource source,
                           SkillStatus status, boolean dbOverridesClasspath) {

    public SkillSummary {
        status = status == null ? SkillStatus.PUBLISHED : status;
    }
}
