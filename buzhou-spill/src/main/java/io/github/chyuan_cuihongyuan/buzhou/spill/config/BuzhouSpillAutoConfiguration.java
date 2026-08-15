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
        // spec 40 §A：加密密钥配置即开（SpillProperties 构造期已 fail-fast 校验合法性）
        io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher cipher = props.encryptionKey() == null
                ? null
                : io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher.fromBase64Key(props.encryptionKey());
        return new SpillModule(Path.of(props.rootDir()), props.previewChars(), props.listPreviewItems(),
                        new io.github.chyuan_cuihongyuan.buzhou.spill.SpillQuota(
                                props.maxTotalBytes(), props.maxFilesPerSession()), cipher)
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
     * impl-30 / spec 13 §core-1 + impl-38 / spec 13 §growth-8：spill 生命周期
     * （phase {@link io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases#SPILL}）。
     * start 时执行启动孤儿扫描（引用会话不存在的文件：报告 + 清理，幂等——live 会话集
     * 来自观测 store 的会话汇总）；并把 spill TTL（deleteExpired）挂进 RetentionSweeper
     * 周期调度（默认 PT1H）。
     */
    @Bean
    public SpillModuleLifecycle spillModuleLifecycle(SpillModule module, SpillProperties props,
                                                     ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores> stores,
                                                     ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper> sweeper) {
        return new SpillModuleLifecycle(module.store(), props.retentionTtl(),
                liveSessionsSupplier(stores.getIfAvailable()), sweeper.getIfAvailable());
    }

    /** live 会话集合（观测 store 分页拉全量；无观测数据源时空集——孤儿扫描按无 live 会话执行）。 */
    private static java.util.function.Supplier<java.util.Set<String>> liveSessionsSupplier(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores stores) {
        return () -> {
            java.util.Set<String> live = new java.util.HashSet<>();
            if (stores == null) {
                return live;
            }
            String cursor = null;
            for (int i = 0; i < 100; i++) {
                var page = stores.observabilityStore().listSessionSummaries(cursor, 500);
                if (page.isEmpty()) {
                    break;
                }
                page.forEach(s -> live.add(io.github.chyuan_cuihongyuan.buzhou.spill.SpillModule
                        .sanitizeComponent(s.sessionId())));
                cursor = String.valueOf(live.size());
            }
            return live;
        };
    }
}
