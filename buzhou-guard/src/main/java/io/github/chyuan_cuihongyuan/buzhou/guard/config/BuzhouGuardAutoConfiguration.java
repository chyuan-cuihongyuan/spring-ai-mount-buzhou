package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * HITL 危险守卫自装配（spec 07 / 09 / ticket 22）。
 *
 * <p>装配 {@link GuardModule}，把 {@link GuardModule#configure()} 产出注册为 {@link RuntimeConfig} bean；
 * 另暴露 {@link GuardAuthApi}（业务侧 REST 授权写回用）。dangerous-tools 清单经
 * {@code buzhou.guard.dangerous-tools} 配置驱动（config-driven，保持模块解耦、不自动耦合 tools）。
 *
 * <p>事实采集器（{@code FactCollectorHook}）属程序化进阶能力，本装配不自动接线；需要时由业务侧
 * 经 {@link GuardModule.Builder#factDefinition} 构建，FactAttachmentRenderer 经 AttachmentRenderer
 * SPI 由 memory 组合（同 todo 渲染器路径）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.guard", name = "enabled", matchIfMissing = true)
public class BuzhouGuardAutoConfiguration {

    @Bean
    public GuardModule guardModule(BuzhouStores stores, Environment env) {
        return GuardModule.fromYml(stores, ConfigMaps.sub(env, "buzhou.guard"));
    }

    @Bean
    public GuardAuthApi guardAuthApi(GuardModule module) {
        return module.authApi();
    }

    @Bean
    public RuntimeConfig guardRuntimeConfig(GuardModule module) {
        return module.configure();
    }

    /**
     * impl-30 / spec 13 §core-1：guard 停机 lifecycle（phase
     * {@link io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases#GUARD}）。
     * 本片为 phase 声明与占位（审计链未在装配面接线、无挂起 flush，诚实边界见
     * {@link GuardModuleLifecycle} Javadoc；flush 钩子属切片 39）。
     */
    @Bean
    public GuardModuleLifecycle guardModuleLifecycle() {
        return new GuardModuleLifecycle();
    }
}
