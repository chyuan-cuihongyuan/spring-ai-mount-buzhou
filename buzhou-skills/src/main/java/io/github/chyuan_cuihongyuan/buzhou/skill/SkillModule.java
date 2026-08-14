package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.manage.SkillAdminApi;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skill 体系模块入口（spec 04）。经 {@link #configure} 返回 {@link RuntimeConfig}，由
 * {@code RuntimeConfig.merge} 与其他机制模块组合；清单渲染器经 {@link #catalogRenderer()}
 * 供 memory 注入视图构建方注入（同 GuardModule.attachmentRenderer() 的跨机制桥接模式）。
 *
 * <p>用法：
 * <pre>{@code
 * SkillModule skills = SkillModule.builder().fromYml(yml).bindingStore(store).build();
 * RuntimeConfig config = RuntimeConfig.merge(
 *     skills.configure(),
 *     MemoryModule.configure(yml, stores, mainModel, summaryModel, null, skills.catalogRenderer()));
 * }</pre>
 */
public final class SkillModule {

    private final boolean enabled;
    private final SkillRegistry registry;
    private final SkillCatalogRenderer catalogRenderer;
    private final LoadSkillTool loadSkillTool;
    private final SkillResourceResolver resourceResolver;
    private final SkillAdminApi adminApi;
    private final SessionBindingIndex bindingIndex;

    private SkillModule(Builder builder) {
        this.enabled = builder.enabled;
        ClasspathSkillScanner scanner = new ClasspathSkillScanner(builder.scanLocations);
        Map<String, ClasspathSkillEntry> classpathSkills = scanner.scan();

        SkillStore dbStore = builder.dbEnabled ? (builder.dbStore != null ? builder.dbStore
                : new InMemorySkillStore()) : null;
        PolicyConfigProvider policyProvider = builder.policyProvider != null ? builder.policyProvider
                : (builder.bindingStore != null ? new StoreBackedPolicyProvider(builder.bindingStore) : null);

        this.registry = new DefaultSkillRegistry(classpathSkills, dbStore, policyProvider,
                builder.dbEnabled, builder.catalogMaxEntries, builder.catalogCacheTtl);
        this.bindingIndex = new SessionBindingIndex();
        this.catalogRenderer = new SkillCatalogRendererImpl(bindingIndex, registry);
        this.loadSkillTool = new LoadSkillTool(registry, bindingIndex);
        this.resourceResolver = new SkillResourceResolverImpl(registry, bindingIndex);
        this.adminApi = new SkillAdminApi(dbStore, classpathSkills, builder.bindingStore,
                this.registry::invalidateCatalogCache);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 从 yml map（前缀 buzhou.skill）解析配置。 */
    public static Builder fromYml(Map<String, Object> ymlConfig) {
        return builder().fromYml(ymlConfig);
    }

    public RuntimeConfig configure() {
        if (!enabled) {
            return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of());
        }
        List<ToolCallback> tools = List.of(loadSkillTool);
        // spawn 时登记 sessionId → (appId, agentName)，供清单渲染器反查；会话关闭时清理防泄漏
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, tools, Map.of(),
                List.of((reg, appId, agentName, sessionId) -> {
                    bindingIndex.register(sessionId, appId, agentName);
                    reg.register("skill-binding-" + sessionId, () -> bindingIndex.remove(sessionId));
                }));
    }

    /** 清单渲染器（供 memory 注入）；模块禁用时返回 null。 */
    public SkillCatalogRenderer catalogRenderer() {
        return enabled ? catalogRenderer : null;
    }

    /**
     * Skill 资源解析器（供 spill 的 {@code read_range} 接管 {@code skill://} 路径）：
     * {@code spillModule.skillResourceResolver(skills.skillResourceResolver())} 装配期接线；
     * 模块禁用时返回 null。
     */
    public SkillResourceResolver skillResourceResolver() {
        return enabled ? resourceResolver : null;
    }

    public SkillRegistry skillRegistry() {
        return registry;
    }

    public SkillAdminApi skillAdminApi() {
        return adminApi;
    }

    public static final class Builder {

        private boolean enabled = true;
        private boolean dbEnabled = false;
        private int catalogMaxEntries = 64;
        /** impl-71 / T96：清单 TTL 缓存（DB 覆盖与负缓存；默认 30s；0 = 关闭）。 */
        private java.time.Duration catalogCacheTtl = java.time.Duration.ofSeconds(30);
        private List<String> scanLocations = List.of(ClasspathSkillScanner.DEFAULT_LOCATION);
        private SkillStore dbStore;
        private PolicyConfigProvider policyProvider;
        private BindingPolicyStore bindingStore;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder dbEnabled(boolean dbEnabled) {
            this.dbEnabled = dbEnabled;
            return this;
        }

        public Builder catalogMaxEntries(int catalogMaxEntries) {
            this.catalogMaxEntries = catalogMaxEntries;
            return this;
        }

        /** impl-71 / T96：清单 TTL 缓存（默认 30s；0 = 关闭，每轮直查 DB）。 */
        public Builder catalogCacheTtl(java.time.Duration catalogCacheTtl) {
            this.catalogCacheTtl = catalogCacheTtl;
            return this;
        }

        public Builder scanLocations(List<String> scanLocations) {
            this.scanLocations = scanLocations;
            return this;
        }

        /** 自带 DB SkillStore（默认提供内存实现）；不设则 db-enabled 时用 InMemorySkillStore。 */
        public Builder dbStore(SkillStore dbStore) {
            this.dbStore = dbStore;
            return this;
        }

        /** 绑定读取来源（优先）；不设则据 bindingStore 读穿。 */
        public Builder policyProvider(PolicyConfigProvider policyProvider) {
            this.policyProvider = policyProvider;
            return this;
        }

        /** 绑定读写存储（管理 API 设绑定 + 默认读穿来源）。 */
        public Builder bindingStore(BindingPolicyStore bindingStore) {
            this.bindingStore = bindingStore;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder fromYml(Map<String, Object> ymlConfig) {
            if (ymlConfig == null || ymlConfig.isEmpty()) {
                return this;
            }
            Object enabledVal = ymlConfig.get("enabled");
            if (enabledVal instanceof Boolean b) {
                this.enabled = b;
            }
            Object dbVal = ymlConfig.get("db-enabled");
            if (dbVal instanceof Boolean b) {
                this.dbEnabled = b;
            }
            Object maxVal = ymlConfig.get("catalog-max-entries");
            if (maxVal instanceof Number n) {
                this.catalogMaxEntries = n.intValue();
            }
            // impl-71 / T96：catalog-cache-ttl（ISO-8601 或秒数；默认 30s；0 = 关闭）
            Object ttlVal = ymlConfig.get("catalog-cache-ttl");
            if (ttlVal instanceof java.time.Duration d) {
                this.catalogCacheTtl = d;
            } else if (ttlVal instanceof Number n) {
                this.catalogCacheTtl = java.time.Duration.ofSeconds(n.longValue());
            } else if (ttlVal instanceof String s && !s.isBlank()) {
                this.catalogCacheTtl = java.time.Duration.parse(s.trim());
            }
            Object locationsVal = ymlConfig.get("scan-locations");
            if (locationsVal instanceof List<?> list) {
                List<String> locations = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String s && !s.isBlank()) {
                        locations.add(s);
                    }
                }
                if (!locations.isEmpty()) {
                    this.scanLocations = List.copyOf(locations);
                }
            }
            return this;
        }

        public SkillModule build() {
            return new SkillModule(this);
        }
    }
}
