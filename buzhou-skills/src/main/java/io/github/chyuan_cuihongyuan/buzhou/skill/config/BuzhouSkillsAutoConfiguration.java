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
public class BuzhouSkillsAutoConfiguration {

    @Bean
    public SkillModule skillModule(Environment env,
                                   ObjectProvider<BindingPolicyStore> bindingStore,
                                   ObjectProvider<PolicyConfigProvider> policyProvider) {
        return SkillModule.fromYml(ConfigMaps.sub(env, "buzhou.skills"))
                .bindingStore(bindingStore.getIfAvailable())
                .policyProvider(policyProvider.getIfAvailable())
                .build();
    }

    @Bean
    public SkillCatalogRenderer skillCatalogRenderer(SkillModule module) {
        return module.catalogRenderer();
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
