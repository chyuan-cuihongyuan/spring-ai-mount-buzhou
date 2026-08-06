package io.github.chyuan_cuihongyuan.buzhou.skill;

/** Skill 来源（spec 04）。 */
public enum SkillSource {
    /** 打包在 jar 内 {@code META-INF/skills/<name>/} 下的内置 Skill。 */
    CLASSPATH,
    /** 经管理 API 上架、存持久层的 DB 动态 Skill；同名覆盖内置。 */
    DB
}
