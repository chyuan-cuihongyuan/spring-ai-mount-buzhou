package io.github.chyuan_cuihongyuan.buzhou.skill;

/**
 * DB 动态 Skill 生命周期状态（spec 04）。
 *
 * <p>仅 {@link #PUBLISHED} 参与运行时解析；{@link #DRAFT}/{@link #DISABLED} 对运行时不可见。
 * classpath 内置 Skill 无状态概念，恒视为已发布。
 */
public enum SkillStatus {
    DRAFT,
    PUBLISHED,
    DISABLED
}
