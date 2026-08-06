package io.github.chyuan_cuihongyuan.buzhou.skill.store;

/**
 * version 乐观锁冲突：{@link SkillStore#save} 携带的 version 与库内现值不一致
 * （管理 API 并发编辑兜底，spec 04）。
 */
public class SkillVersionConflictException extends IllegalStateException {

    public SkillVersionConflictException(String name, int submittedVersion, int storeVersion) {
        super("Skill version 冲突：" + name
                + "（提交 version=" + submittedVersion + "，库内 version=" + storeVersion + "）——请重读后再改");
    }
}
