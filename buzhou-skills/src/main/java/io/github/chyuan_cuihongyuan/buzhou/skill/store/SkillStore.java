package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;

import java.util.List;
import java.util.Optional;

/**
 * DB 动态 Skill 持久化（spec 04 三张表的数据访问门面）。
 *
 * <p>内核默认提供内存实现（{@link InMemorySkillStore}）；JDBC/Redis 实现可由 store 实现模块
 * 补齐（与持久化五 SPI 同模式）。classpath 内置 Skill 不经此接口。
 */
public interface SkillStore {

    // ---- Skill 主表 ----

    /** 按名查找（任意状态）；管理 API 用。 */
    Optional<DbSkillRecord> findByName(String name);

    /** 按名查找已上架（{@link SkillStatus#PUBLISHED}）；运行时解析用（DB 覆盖内置）。 */
    Optional<DbSkillRecord> findPublished(String name);

    /** 全量列表（含 DRAFT/DISABLED）；管理 API 用。 */
    List<DbSkillRecord> findAll();

    /**
     * 新建或更新（按 name upsert）；返回落库后的记录（含新 version/时间戳）。
     *
     * <p>乐观锁契约：更新时 {@code record.version()} 须等于库内现值（新建传 0 且库内不存在），
     * 不一致抛 {@link SkillVersionConflictException}——并发编辑以先到者为准，后到者重读再改。
     */
    DbSkillRecord save(DbSkillRecord record);

    /** 删除（仅 DB Skill）；返回是否确有删除。删除后同名内置自动恢复可见。 */
    boolean deleteByName(String name);

    // ---- 资源表 ----

    Optional<DbSkillResourceRecord> findResource(String skillName, String relativePath);

    List<DbSkillResourceRecord> findResources(String skillName);

    DbSkillResourceRecord saveResource(DbSkillResourceRecord record);

    /** 删除某 Skill 的全部资源（删 Skill 时级联）。 */
    void deleteResources(String skillName);
}
