package io.github.chyuan_cuihongyuan.buzhou.tools.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 内置原子工具自装配（spec 06 / 09 / ticket 22）。
 *
 * <p>装配 {@link ToolsModule}，把 {@link ToolsModule#configure()} 产出（含 serialGroups / idempotent
 * 元数据，无损保留进合并）注册为 {@link RuntimeConfig} bean；todo 启用时另把
 * {@code TodoAttachmentRenderer} 注册为 {@link AttachmentRenderer} bean，经 core SPI 由 memory 组合注入。
 *
 * <p>默认开关矩阵（spec 06）：read_file / todo 默认开，write_file / run_command / http_request
 * 默认关（绑定级 opt-in，配置经 {@code buzhou.tools.*}）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.tools", name = "enabled", matchIfMissing = true)
public class BuzhouToolsAutoConfiguration {

    @Bean
    public ToolsModule toolsModule(BuzhouStores stores, Environment env) {
        return ToolsModule.fromYml(stores.sessionStateStore(), ConfigMaps.sub(env, "buzhou.tools")).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "buzhou.tools.todo", name = "enabled", matchIfMissing = true)
    public AttachmentRenderer todoAttachmentRenderer(ToolsModule module) {
        return module.todoAttachmentRenderer();
    }

    @Bean
    public RuntimeConfig toolsRuntimeConfig(ToolsModule module) {
        return module.configure();
    }
}
