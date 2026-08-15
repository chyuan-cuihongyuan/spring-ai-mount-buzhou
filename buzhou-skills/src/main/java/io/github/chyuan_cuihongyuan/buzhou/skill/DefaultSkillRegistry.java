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
    /** impl-71 / T96：清单 TTL 缓存（DB 覆盖与未命中负缓存；classpath 命中本就是 map 查找不缓存）。 */
    private final java.time.Duration catalogCacheTtl;
    private final java.util.concurrent.ConcurrentHashMap<String, CachedResolve> resolveCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private record CachedResolve(Optional<Skill> skill, long expiresAtNanos) {
    }

    public DefaultSkillRegistry(Map<String, ClasspathSkillEntry> classpathSkills,
                                SkillStore dbStore, PolicyConfigProvider policyProvider,
                                boolean dbEnabled, int catalogMaxEntries) {
        this(classpathSkills, dbStore, policyProvider, dbEnabled, catalogMaxEntries,
                java.time.Duration.ofSeconds(30));
    }

    public DefaultSkillRegistry(Map<String, ClasspathSkillEntry> classpathSkills,
                                SkillStore dbStore, PolicyConfigProvider policyProvider,
                                boolean dbEnabled, int catalogMaxEntries,
                                java.time.Duration catalogCacheTtl) {
        this.classpathSkills = classpathSkills == null ? Map.of() : Map.copyOf(classpathSkills);
        this.dbStore = dbStore;
        this.policyProvider = policyProvider;
        this.dbEnabled = dbEnabled && dbStore != null;
        this.catalogMaxEntries = catalogMaxEntries <= 0 ? 64 : catalogMaxEntries;
        this.catalogCacheTtl = catalogCacheTtl == null || catalogCacheTtl.isNegative()
                ? java.time.Duration.ofSeconds(30) : catalogCacheTtl;
    }

    /** impl-71 / T96：失效清单缓存（admin 变更后调用，写立即可见不等 TTL）。 */
    public void invalidateCatalogCache() {
        resolveCache.clear();
    }

    @Override
    public List<SkillMetadata> listFor(String appId, String agentName) {
        return listForPage(appId, agentName).entries();
    }

    /** spec 37 §A / T132：不截断全集（检索源——不受 catalog-max-entries 限制）。 */
    @Override
    public List<SkillMetadata> listAllFor(String appId, String agentName) {
        List<SkillMetadata> catalog = new ArrayList<>();
        for (String name : candidatesFor(appId, agentName)) {
            resolve(name).ifPresent(skill ->
                    catalog.add(new SkillMetadata(skill.name(), skill.description(),
                            skill.allowedTools(), skill.source())));
        }
        return List.copyOf(catalog);
    }

    /** spec 35 §B / T119：截断 + 溢出计数（渲染器据此提示「另有 N 个未列出」）。 */
    @Override
    public CatalogPage listForPage(String appId, String agentName) {
        List<String> candidates = candidatesFor(appId, agentName);
        List<SkillMetadata> catalog = new ArrayList<>();
        for (String name : candidates) {
            if (catalog.size() >= catalogMaxEntries) {
                break; // 先判后加：溢出计数以 candidates 全量为准
            }
            resolve(name).ifPresent(skill ->
                    catalog.add(new SkillMetadata(skill.name(), skill.description(),
                            skill.allowedTools(), skill.source())));
        }
        return new CatalogPage(List.copyOf(catalog), candidates.size());
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
        // impl-71 / T96：classpath 命中零成本直返；DB 路径（覆盖查询 + 未命中）走 TTL 缓存——
        // 此前 listFor 每轮对每技能都打一次 DB（N 技能 = N 次/轮）。ttl=0 显式关闭缓存。
        ClasspathSkillEntry entry = classpathSkills.get(name);
        if (dbEnabled) {
            long now = System.nanoTime();
            CachedResolve cached = resolveCache.get(name);
            if (cached != null && cached.expiresAtNanos() > now) {
                // DB 覆盖优先于 classpath：缓存命中（含负缓存）直接定论
                return cached.skill();
            }
            Optional<Skill> resolved = dbOverride(name).map(this::toSkill);
            if (resolved.isEmpty() && entry != null) {
                resolved = Optional.of(entry.skill());
            }
            resolveCache.put(name, new CachedResolve(resolved,
                    now + catalogCacheTtl.toNanos()));
            return resolved;
        }
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
