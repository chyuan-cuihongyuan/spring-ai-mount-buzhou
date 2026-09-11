package io.github.chyuan_cuihongyuan.buzhou.skill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Skill 体系自装配（spec 04 / 09 / ticket 22）。
 *
 * <p>装配 {@link SkillModule}，把 {@link SkillModule#configure()} 产出注册为 {@link RuntimeConfig} bean；
 * 另把 {@link SkillCatalogRenderer}（供 memory 注入清单）与 {@link SkillResourceResolver}
 * （供 spill 的 {@code read_range} 接管 {@code skill://}）注册为 bean，经 core SPI 跨机制桥接，
 * 不产生 feature→feature 编译边。绑定来源（DB）与策略提供方经 {@link ObjectProvider} 可空。
 *
 * <p>开关与配置统一用 {@code buzhou.skills.*}（spec 09 模块开关表用复数；既有
 * {@code SkillModule#fromYml} 读 {@code enabled/db-enabled/catalog-max-entries/scan-locations}）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.skills", name = "enabled", matchIfMissing = true)
@org.springframework.boot.context.properties.EnableConfigurationProperties(BuzhouSkillsProperties.class)
public class BuzhouSkillsAutoConfiguration {

    /**
     * impl-51 / spec 14 §G：接线持久化 SkillStore bean（容器内存在
     * {@link io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore} 实现——如
     * {@code JdbcSkillStore}/{@code RedisSkillStore} 的 @Bean——即注入模块）。
     * 此前装配路径无法接入 store，DB 动态 Skill 只能手工编码。
     */
    @Bean
    public SkillModule skillModule(Environment env,
                                   BuzhouSkillsProperties properties,
                                   ObjectProvider<BindingPolicyStore> bindingStore,
                                   ObjectProvider<PolicyConfigProvider> policyProvider,
                                   ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore> skillStore,
                                   ObjectProvider<org.springframework.ai.embedding.EmbeddingModel> embeddingModels) {
        io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore store = skillStore.getIfAvailable();
        io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule.Builder builder =
                SkillModule.fromYml(ConfigMaps.sub(env, "buzhou.skills"))
                        .bindingStore(bindingStore.getIfAvailable())
                        .policyProvider(policyProvider.getIfAvailable())
                        // spec 59 §A / T266：语义排序嵌入模型（optional——enabled=true 而无 bean
                        // 在 build() fail-fast 带修法，与语义缓存同口径）
                        .embeddingModel(embeddingModels.getIfAvailable());
        if (store != null) {
            // 显式 store bean = 用户意图启用 DB 动态 Skill（除非显式 db-enabled=false；impl-66 正规化注入）
            builder.dbStore(store).dbEnabled(properties.dbEnabled());
        }
        return builder.build();
    }

    @Bean
    public SkillCatalogRenderer skillCatalogRenderer(SkillModule module) {
        // spec 629 / T908：漂移看门狗搭渲染节拍（617 接线——零调度；漂移即 WARN + 计数）
        io.github.chyuan_cuihongyuan.buzhou.skill.SkillCatalogDriftWatcher watcher =
                new io.github.chyuan_cuihongyuan.buzhou.skill.SkillCatalogDriftWatcher(payload ->
                        System.getLogger(BuzhouSkillsAutoConfiguration.class.getName()).log(
                                System.Logger.Level.WARNING,
                                "技能目录漂移（skill.catalog.drifted）：{0}", payload));
        return module.catalogRendererWithDriftWatcher(watcher);
    }

    @Bean
    public SkillResourceResolver skillResourceResolver(SkillModule module) {
        return module.skillResourceResolver();
    }

    @Bean
    public RuntimeConfig skillsRuntimeConfig(SkillModule module) {
        return module.configure();
    }
}
