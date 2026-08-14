package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillGuardModule;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Spill 长内容治理自装配（spec 02 / 09 / ticket 22）。
 *
 * <p>装配 {@link SpillModule}（读侧 offload + {@code read_range} 回读工具）与 {@link SpillGuardModule}
 * （写侧 onload + 只读护栏 + 编辑工具），各自 {@code configure()} 产出注册为独立 {@link RuntimeConfig}
 * bean（命名 {@code spillRuntimeConfig} / {@code spillGuardRuntimeConfig}），供 core 收集合并。
 *
 * <p>{@link SkillResourceResolver}（skills 提供，接管 {@code skill://} 路径）经 {@link ObjectProvider} 注入，
 * 缺失时 {@code read_range} 返回接线提示文本。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.spill", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpillProperties.class)
public class BuzhouSpillAutoConfiguration {

    @Bean
    public SpillModule spillModule(SpillProperties props, ObjectProvider<SkillResourceResolver> resolver) {
        return new SpillModule(Path.of(props.rootDir()), props.previewChars(), props.listPreviewItems())
                .skillResourceResolver(resolver.getIfAvailable());
    }

    @Bean
    public RuntimeConfig spillRuntimeConfig(SpillModule module) {
        return module.configure();
    }

    @Bean
    public RuntimeConfig spillGuardRuntimeConfig(SpillModule module, SpillProperties props) {
        SpillGuardModule.Builder builder = SpillGuardModule.fromModule(module, Path.of(props.sandboxRoot()))
                .thresholdChars(props.thresholdChars())
                .onloadEnabled(props.onloadEnabled())
                .copyOnWriteEnabled(props.copyOnWriteEnabled())
                .offloadEnabled(props.offloadEnabled())
                .editingToolsEnabled(props.editingToolsEnabled());
        if (props.thresholdTokens() != null) {
            builder.thresholdTokens(props.thresholdTokens());
        }
        return builder.build().configure();
    }

    /**
     * impl-30 / spec 13 §core-1：spill 停机 lifecycle（phase
     * {@link io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases#SPILL}）。
     * 本片为 phase 声明与占位（spill 无可关闭资源，诚实边界见
     * {@link SpillModuleLifecycle} Javadoc）。
     */
    @Bean
    public SpillModuleLifecycle spillModuleLifecycle() {
        return new SpillModuleLifecycle();
    }
}
