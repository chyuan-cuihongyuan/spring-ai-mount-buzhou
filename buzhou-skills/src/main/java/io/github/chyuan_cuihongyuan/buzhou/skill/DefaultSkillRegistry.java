package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillResourceRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link SkillRegistry} 默认实现：DB（PUBLISHED）&gt; classpath 解析，绑定裁剪清单。
 *
 * <p>线程安全：classpath 扫描结果构造期固定，DB/store/policy 读取即时进行（上架/解绑/改绑定
 * 下一轮即生效，无需重建）。
 */
public class DefaultSkillRegistry implements SkillRegistry {

    private final Map<String, ClasspathSkillEntry> classpathSkills;
    private final SkillStore dbStore;
    private final PolicyConfigProvider policyProvider;
    private final boolean dbEnabled;
    private final int catalogMaxEntries;

    public DefaultSkillRegistry(Map<String, ClasspathSkillEntry> classpathSkills,
                                SkillStore dbStore, PolicyConfigProvider policyProvider,
                                boolean dbEnabled, int catalogMaxEntries) {
        this.classpathSkills = classpathSkills == null ? Map.of() : Map.copyOf(classpathSkills);
        this.dbStore = dbStore;
        this.policyProvider = policyProvider;
        this.dbEnabled = dbEnabled && dbStore != null;
        this.catalogMaxEntries = catalogMaxEntries <= 0 ? 64 : catalogMaxEntries;
    }

    @Override
    public List<SkillMetadata> listFor(String appId, String agentName) {
        List<String> candidates = candidatesFor(appId, agentName);
        List<SkillMetadata> catalog = new ArrayList<>();
        for (String name : candidates) {
            resolve(name).ifPresent(skill ->
                    catalog.add(new SkillMetadata(skill.name(), skill.description(),
                            skill.allowedTools(), skill.source())));
            if (catalog.size() >= catalogMaxEntries) {
                break;
            }
        }
        return List.copyOf(catalog);
    }

    @Override
    public Optional<Skill> load(String appId, String agentName, String name) {
        return resolve(name);
    }

    @Override
    public Optional<String> loadResource(String appId, String agentName, String skillName,
                                         String relativePath) {
        if (skillName == null || relativePath == null) {
            return Optional.empty();
        }
        // DB 覆盖优先：DB published 命中则取 DB 资源
        if (dbOverride(skillName).isPresent()) {
            return dbStore.findResource(skillName, relativePath)
                    .map(DbSkillResourceRecord::content);
        }
        ClasspathSkillEntry entry = classpathSkills.get(skillName);
        if (entry != null) {
            return Optional.ofNullable(entry.resourceContents().get(relativePath));
        }
        return Optional.empty();
    }

    @Override
    public boolean isVisibleFor(String appId, String agentName, String name) {
        // 不走 listFor：清单展示上限只约束提示词渲染，不约束可见性判定
        return candidatesFor(appId, agentName).contains(name) && resolve(name).isPresent();
    }

    /** DB-PUBLISHED &gt; classpath 的纯名解析（绑定无关；可见性由 isVisibleFor/listFor 约束）。 */
    private Optional<Skill> resolve(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Optional<DbSkillRecord> db = dbOverride(name);
        if (db.isPresent()) {
            return Optional.of(toSkill(db.get()));
        }
        ClasspathSkillEntry entry = classpathSkills.get(name);
        return entry == null ? Optional.empty() : Optional.of(entry.skill());
    }

    /** DB 动态 Skill 覆盖查询（PUBLISHED 才参与解析）；未启用 DB 时恒空。 */
    private Optional<DbSkillRecord> dbOverride(String name) {
        return dbEnabled ? dbStore.findPublished(name) : Optional.empty();
    }

    private List<String> candidatesFor(String appId, String agentName) {
        List<String> bound = boundSkillNames(appId, agentName);
        return bound != null ? bound : new ArrayList<>(classpathSkills.keySet());
    }

    private List<String> boundSkillNames(String appId, String agentName) {
        if (policyProvider == null) {
            return null;
        }
        List<String> skillNames = policyProvider.getBindingPolicy(appId, agentName).skillNames();
        return skillNames.isEmpty() ? null : skillNames;
    }

    private Skill toSkill(DbSkillRecord record) {
        List<SkillResource> resources = List.of();
        if (dbStore != null) {
            resources = dbStore.findResources(record.name()).stream()
                    .map(r -> new SkillResource(r.relativePath(), r.sizeBytes(), r.mediaType()))
                    .toList();
        }
        return new Skill(record.name(), record.description(), record.allowedTools(),
                record.body(), resources, SkillSource.DB);
    }
}
