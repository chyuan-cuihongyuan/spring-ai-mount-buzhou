package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSource;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillResourceRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill 管理门面（spec 04 管理 API 的程序化形态，对应 REST {@code /api/skills}）。
 *
 * <p>提供 DB 动态 Skill 的 CRUD、上架/下架状态流转、资源上传，以及 (appId, agentName) →
 * skillName 绑定关系管理（绑定存 {@link BindingPolicyStore}，并入 PolicyConfigProvider 体系）。
 * 内置 Skill 只读。REST 控制器（dashboard，ticket 17）薄包一层即可暴露为 HTTP。
 */
public class SkillAdminApi {

    private final SkillStore dbStore;
    private final Map<String, ClasspathSkillEntry> classpathSkills;
    private final BindingPolicyStore bindingStore;
    /** impl-71 / T96：变更后失效清单 TTL 缓存（null = 无缓存可失效）。 */
    private final Runnable catalogCacheInvalidator;

    public SkillAdminApi(SkillStore dbStore,
                         Map<String, ClasspathSkillEntry> classpathSkills,
                         BindingPolicyStore bindingStore) {
        this(dbStore, classpathSkills, bindingStore, null);
    }

    public SkillAdminApi(SkillStore dbStore,
                         Map<String, ClasspathSkillEntry> classpathSkills,
                         BindingPolicyStore bindingStore,
                         Runnable catalogCacheInvalidator) {
        this.dbStore = dbStore;
        this.classpathSkills = classpathSkills == null ? Map.of() : Map.copyOf(classpathSkills);
        this.bindingStore = bindingStore;
        this.catalogCacheInvalidator = catalogCacheInvalidator;
    }

    /** 变更后失效清单缓存（admin 写立即可见，不等 TTL）。 */
    private void invalidateCatalogCache() {
        if (catalogCacheInvalidator != null) {
            catalogCacheInvalidator.run();
        }
    }

    // ---- Skill CRUD ----

    /** 新建 DB Skill（初始 DRAFT）；同名 DB Skill 已存在时拒绝（防静默覆盖）。 */
    public DbSkillRecord create(String name, String description, String body,
                                List<String> allowedTools, String createdBy) {
        requireDbEnabled();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("skill name 不能为空");
        }
        if (dbStore.findByName(name).isPresent()) {
            throw new IllegalArgumentException("DB Skill 已存在：" + name + "（请用 update 编辑）");
        }
        DbSkillRecord draft = new DbSkillRecord(null, name, description == null ? "" : description,
                allowedTools, body == null ? "" : body, SkillStatus.DRAFT, createdBy,
                null, null, 0);
        return dbStore.save(draft);
    }

    /** 编辑 DB Skill（仅 DB；内置只读）。 */
    public DbSkillRecord update(String name, String description, String body, List<String> allowedTools) {
        requireDbEnabled();
        DbSkillRecord existing = dbStore.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("DB Skill 不存在：" + name));
        DbSkillRecord updated = new DbSkillRecord(existing.id(), existing.name(),
                description == null ? existing.description() : description,
                allowedTools == null ? existing.allowedTools() : allowedTools,
                body == null ? existing.body() : body, existing.status(), existing.createdBy(),
                existing.createdAt(), existing.updatedAt(), existing.version());
        DbSkillRecord saved = dbStore.save(updated);
        invalidateCatalogCache();
        return saved;
    }

    /**
     * 上架：DRAFT → PUBLISHED；DISABLED → PUBLISHED（重新上架，spec 04 推演）。
     * 已上架时拒绝（重复上架视为操作错误）。
     */
    public DbSkillRecord publish(String name) {
        requireDbEnabled();
        DbSkillRecord existing = mustFind(name);
        if (existing.status() == SkillStatus.PUBLISHED) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SKILL_OPERATION_INVALID,
                    "Skill 已是上架状态：" + name);
        }
        return transition(existing, SkillStatus.PUBLISHED);
    }

    /** 下架：PUBLISHED → DISABLED（运行时不可见，同名内置自动恢复）；非上架状态拒绝。 */
    public DbSkillRecord disable(String name) {
        requireDbEnabled();
        DbSkillRecord existing = mustFind(name);
        if (existing.status() != SkillStatus.PUBLISHED) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SKILL_OPERATION_INVALID,
                    "仅上架状态可下架（当前 " + existing.status() + "）：" + name);
        }
        return transition(existing, SkillStatus.DISABLED);
    }

    /** 删除 DB Skill（仅 DB；删除后同名内置自动恢复可见）。 */
    public boolean delete(String name) {
        requireDbEnabled();
        boolean deleted = dbStore.deleteByName(name);
        if (deleted) {
            invalidateCatalogCache();
        }
        return deleted;
    }

    private DbSkillRecord transition(DbSkillRecord existing, SkillStatus target) {
        DbSkillRecord updated = new DbSkillRecord(existing.id(), existing.name(),
                existing.description(), existing.allowedTools(), existing.body(), target,
                existing.createdBy(), existing.createdAt(), existing.updatedAt(), existing.version());
        DbSkillRecord saved = dbStore.save(updated);
        invalidateCatalogCache();
        return saved;
    }

    private DbSkillRecord mustFind(String name) {
        return dbStore.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("DB Skill 不存在：" + name));
    }

    // ---- 资源 ----

    public DbSkillResourceRecord uploadResource(String skillName, String relativePath,
                                                String content, String mediaType) {
        requireDbEnabled();
        if (skillName == null || relativePath == null) {
            throw new IllegalArgumentException("skillName/relativePath 不能为空");
        }
        return dbStore.saveResource(new DbSkillResourceRecord(null, skillName, relativePath,
                mediaType, content == null ? "" : content,
                content == null ? 0 : content.length(), null));
    }

    // ---- 合并视图 ----

    /** DB 与内置合并展示（标注覆盖关系）；DB 条目含 DRAFT/DISABLED。 */
    public List<SkillSummary> listAll() {
        Map<String, SkillSummary> merged = new LinkedHashMap<>();
        classpathSkills.forEach((name, entry) -> merged.put(name, new SkillSummary(
                name, entry.skill().description(), SkillSource.CLASSPATH, SkillStatus.PUBLISHED, false)));
        for (DbSkillRecord db : dbStore.findAll()) {
            boolean overrides = classpathSkills.containsKey(db.name());
            merged.put(db.name(), new SkillSummary(db.name(), db.description(), SkillSource.DB,
                    db.status(), overrides));
        }
        return List.copyOf(merged.values());
    }

    public Optional<DbSkillRecord> findDb(String name) {
        return dbStore.findByName(name);
    }

    // ---- 绑定 ----

    /** 当前 (appId, agentName) 绑定的 skillName 清单（未绑定为空）。 */
    public List<String> getBinding(String appId, String agentName) {
        if (bindingStore == null) {
            return List.of();
        }
        return bindingStore.find(appId, agentName)
                .map(BindingPolicy::skillNames)
                .orElse(List.of());
    }

    /**
     * 设绑定：(appId, agentName) → skillName 清单（整体替换；并入 PolicyConfigProvider）。
     * impl-51：skillName 须真实存在（DB 任意状态或 classpath 内置）——此前可绑定不存在的名，
     * 运行时静默不生效。
     */
    public void setBinding(String appId, String agentName, List<String> skillNames) {
        if (bindingStore == null) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.CONFIG_INVALID,
                    "未配置 BindingPolicyStore，不支持绑定管理");
        }
        List<String> requested = skillNames == null ? List.of() : skillNames;
        java.util.Set<String> known = new java.util.HashSet<>();
        if (dbStore != null) {
            dbStore.findAll().forEach(r -> known.add(r.name()));
        }
        if (classpathSkills != null) {
            known.addAll(classpathSkills.keySet());
        }
        for (String name : requested) {
            if (!known.contains(name)) {
                throw new IllegalArgumentException(
                        "绑定失败：skill \"" + name + "\" 不存在（DB 与 classpath 均未找到）");
            }
        }
        BindingPolicy existing = bindingStore.find(appId, agentName)
                .orElse(BindingPolicy.empty(appId, agentName));
        BindingPolicy updated = new BindingPolicy(appId, agentName, existing.mechanismOverrides(),
                new ArrayList<>(skillNames == null ? List.of() : skillNames),
                existing.mcpServers(), existing.version() + 1);
        bindingStore.save(updated);
    }

    private void requireDbEnabled() {
        if (dbStore == null) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.CONFIG_INVALID,
                    "未配置 DB SkillStore，DB 动态 Skill 不可用（buzhou.skill.db-enabled）");
        }
    }
}
