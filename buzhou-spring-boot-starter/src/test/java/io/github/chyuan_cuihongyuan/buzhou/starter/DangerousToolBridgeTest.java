package io.github.chyuan_cuihongyuan.buzhou.starter;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.DangerousToolRegistry;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.BuzhouGuardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule;
import io.github.chyuan_cuihongyuan.buzhou.tools.config.BuzhouToolsAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1508 / T2267–T2268：危险工具默认 HITL 自动带入桥（design-incompleteness
 * S2 修复）E2E——tools（write_file opt-in 开）+ guard 同装时，GuardModule 的最终
 * 危险清单含 write_file 默认条目（requiredState=confirm_write_file）；显式 yml
 * 条目优先不重复；auto-dangerous-bridge=false 逃生。
 */
class DangerousToolBridgeTest {

    @AfterEach
    void tearDown() {
        DangerousToolRegistry.reset(); // 静态注册表测试隔离
    }

    /** 最小上下文：core stores + tools（write_file 开）+ guard。 */
    private ApplicationContextRunner runnerWith(String... props) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BuzhouCoreAutoConfiguration.class,
                        BuzhouToolsAutoConfiguration.class,
                        BuzhouGuardAutoConfiguration.class))
                .withUserConfiguration(StoresConfig.class)
                .withPropertyValues(props);
    }

    /** ① opt-in 开 write_file：自动带入默认 HITL 条目（S2 承诺恢复）。 */
    @Test
    void optInWriteFileShouldAutoEnrolDefaultHitlEntry() {
        runnerWith("buzhou.tools.write-file.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ToolsModule.class);
                    assertThat(context).hasSingleBean(GuardModule.class);
                    // 灌注面：tools 装配后注册表含 write_file
                    assertThat(DangerousToolRegistry.registered()).contains("write_file");
                    // 消费面：guard 最终清单含默认条目（三参默认形态）
                    GuardModule guard = context.getBean(GuardModule.class);
                    assertThat(guard.dangerousTools())
                            .anyMatch(e -> "write_file".equals(e.name())
                                    && "confirm_write_file".equals(e.requiredState()));
                });
    }

    /**
     * ② yml 显式条目优先：同名不重复登记（显式 requiredState 保留）。
     * 用 MapPropertySource 塞聚合 List 形态（YAML 文件源的等价结构）——properties
     * 源的 indexed 写法经 ConfigMaps.sub 会绑成 Map 形态而非 List（既有坑，T2267
     * 轮实证入档，候选池主题），此处测桥的去重语义本身。
     */
    @Test
    void explicitYmlEntryShouldWinOverAutoDefault() {
        runnerWith("buzhou.tools.write-file.enabled=true")
                .withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                        new org.springframework.core.env.MapPropertySource("test-dangerous-yml",
                                java.util.Map.of("buzhou.guard.dangerous-tools",
                                        java.util.List.of(java.util.Map.of(
                                                "name", "write_file",
                                                "required-state", "custom_state",
                                                "hint", "自定义提示"))))))
                .run(context -> {
                    GuardModule guard = context.getBean(GuardModule.class);
                    long writeFileEntries = guard.dangerousTools().stream()
                            .filter(e -> "write_file".equals(e.name())).count();
                    // 显式一条，自动并入被去重
                    assertThat(writeFileEntries).isEqualTo(1);
                    assertThat(guard.dangerousTools())
                            .anyMatch(e -> "write_file".equals(e.name())
                                    && "custom_state".equals(e.requiredState()));
                });
    }

    /** ③ 逃生门：auto-dangerous-bridge=false 零并入。 */
    @Test
    void bridgeDisabledShouldEnrolNothing() {
        runnerWith("buzhou.tools.write-file.enabled=true",
                "buzhou.guard.auto-dangerous-bridge=false")
                .run(context -> {
                    GuardModule guard = context.getBean(GuardModule.class);
                    assertThat(guard.dangerousTools())
                            .noneMatch(e -> "write_file".equals(e.name()));
                });
    }

    @Configuration
    static class StoresConfig {
        @Bean
        BuzhouStores buzhouStores() {
            return io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        }
    }
}
