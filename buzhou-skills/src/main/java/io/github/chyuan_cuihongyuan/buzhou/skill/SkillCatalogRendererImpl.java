package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;

import java.util.List;
import java.util.Optional;

/**
 * {@link SkillCatalogRenderer} 实现：据 sessionId 反查 (appId, agentName) 绑定，
 * 经 {@link SkillRegistry#listFor} 取可见清单，渲染为系统提示词尾部注入文本（spec 04）。
 *
 * <p>每轮注入视图构建时现取——上架/解绑/改绑定下一轮即生效，无需重建会话。
 *
 * <p><b>语义排序（spec 59 §A / T265，默认关）</b>：ranker 非 null 且 queryHint 非空时，
 * 候选取不截断全集（listAllFor）按相似度排序后应用目录预算——预算内保最相关技能；
 * 溢出计数与提示口径不变（只改变「哪 N 个进入预算」）。无 hint / 排序失败回退注册序。
 */
public class SkillCatalogRendererImpl implements SkillCatalogRenderer {

    private final SessionBindingIndex index;
    private final SkillRegistry registry;
    /** 语义排序器（null = 禁用——注册序，行为与历史版本一致）。 */
    private final SkillRanker ranker;
    /** 目录注入预算（排序路径本地截断用；与 registry 同值）。 */
    private final int catalogMaxEntries;
    /** spec 629 / T908：目录漂移看门狗（null = 不监测）。 */
    private final SkillCatalogDriftWatcher driftWatcher;
    /** 渲染缓存（spec 168 / T521）：渲染是目录内容纯函数，按内容寻址复用——
     * 目录每轮注入是热点，命中率 = 目录稳定性信号。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache<String>
            renderCache = io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache.create();

    public SkillCatalogRendererImpl(SessionBindingIndex index, SkillRegistry registry) {
        this(index, registry, null, Integer.MAX_VALUE);
    }

    /** spec 59 §A / T265：带语义排序与预算的构造（ranker null = 禁用）。 */
    public SkillCatalogRendererImpl(SessionBindingIndex index, SkillRegistry registry,
            SkillRanker ranker, int catalogMaxEntries) {
        this(index, registry, ranker, catalogMaxEntries, null);
    }

    /**
     * spec 629 / T908：带目录漂移看门狗的构造（watcher null = 不监测——零变化）。
     * 渲染节拍即巡查宿主：每轮 render 顺带 check（目录指纹漂移即事件+计数——617 接线）。
     */
    public SkillCatalogRendererImpl(SessionBindingIndex index, SkillRegistry registry,
            SkillRanker ranker, int catalogMaxEntries,
            SkillCatalogDriftWatcher driftWatcher) {
        this.index = index;
        this.registry = registry;
        this.ranker = ranker;
        this.catalogMaxEntries = catalogMaxEntries;
        this.driftWatcher = driftWatcher;
    }

    /** 渲染缓存统计（命中率 = 目录稳定性信号；测试/观测面，spec 168 / T521）。 */
    public io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache.Stats renderCacheStats() {
        return renderCache.stats();
    }

    @Override
    public Optional<String> renderCatalog(String sessionId) {
        SessionBindingIndex.Binding binding = index.get(sessionId).orElse(null);
        if (binding == null) {
            return Optional.empty();
        }
        SkillRegistry.CatalogPage page = registry.listForPage(binding.appId(), binding.agentName());
        return renderEntries(page.entries(), page.total() - page.entries().size());
    }

    /** spec 59 §A / T264：带问法路径——排序后本地截断（预算内保最相关；口径与 listForPage 一致）。 */
    @Override
    public Optional<String> renderCatalog(String sessionId, String queryHint) {
        if (ranker == null) {
            return renderCatalog(sessionId); // 禁用：零行为变化
        }
        SessionBindingIndex.Binding binding = index.get(sessionId).orElse(null);
        if (binding == null) {
            return Optional.empty();
        }
        List<SkillMetadata> all = registry.listAllFor(binding.appId(), binding.agentName());
        if (all.isEmpty()) {
            return Optional.empty();
        }
        List<SkillMetadata> ranked = ranker.rank(all, queryHint);
        List<SkillMetadata> shown = ranked.size() > catalogMaxEntries
                ? ranked.subList(0, catalogMaxEntries) : ranked;
        return renderEntries(shown, all.size() - shown.size());
    }

    private Optional<String> renderEntries(List<SkillMetadata> catalog, int overflow) {
        if (catalog.isEmpty()) {
            return Optional.empty();
        }
        // spec 110 §A / T401：目录注入遥测（injected 每注入 +1；overflow 两值 tag——
        // 截断频率提示 catalog-max-entries 是否过小）
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.skills.catalog-injected");
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .counter("buzhou.skills.catalog-overflow", "outcome",
                        overflow > 0 ? "truncated" : "fit");
        // spec 168 §A / T521：渲染缓存（PromptPrefixCache 首个内置消费方）——
        // 渲染是目录内容的纯函数，按「name+description+overflow 规范形」内容寻址：
        // 同目录每轮注入命中（目录每轮注入是热点），上架/改文案即换键自然失效；
        // 命中率 = 目录稳定性信号（与 spec 126「命中率即能力」同口径）
        StringBuilder canonical = new StringBuilder();
        for (SkillMetadata meta : catalog) {
            canonical.append(meta.name()).append('|')
                    .append(meta.description() == null ? "" : meta.description()).append('\n');
        }
        canonical.append("#overflow=").append(overflow);
        String key = io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache
                .keyOf(canonical.toString());
        // spec 629 / T908：渲染节拍巡查（首拍建基线；漂移即 617 的事件+计数——零调度）
        if (driftWatcher != null) {
            driftWatcher.check(catalog);
        }
        return renderCache.getOrLoad(key, () -> render_uncached(catalog, overflow));
    }

    private String render_uncached(List<SkillMetadata> catalog, int overflow) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 可用技能（Skill Catalog）\n");
        sb.append("以下技能可按需调用 load_skill(name) 加载正文（name 即清单首列）：\n");
        for (SkillMetadata meta : catalog) {
            sb.append("- ").append(meta.name());
            if (meta.description() != null && !meta.description().isBlank()) {
                sb.append(": ").append(meta.description());
            }
            sb.append('\n');
        }
        if (overflow > 0) {
            sb.append("（另有 ").append(overflow)
                    .append(" 个技能因目录注入上限未列出——如需加载其正文，")
                    .append("请运维调整绑定关系或提高 buzhou.skills.catalog-max-entries）\n");
        }
        return sb.toString().strip();
    }
}
