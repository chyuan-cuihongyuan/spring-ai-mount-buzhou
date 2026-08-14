package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.JdbcBuzhouRecoveryStores;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC store 装配测试（ticket 22 + ticket 31）：store.type=jdbc + H2 内嵌 DataSource
 * （hermetic，无 Docker）——核心 store 装配 + 恢复设施按开关装配。
 */
class BuzhouJdbcStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouJdbcStoreAutoConfiguration.class))
            .withBean(DataSource.class, () -> new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2).build());

    @Test
    void jdbcStoreAssemblesOnJdbcType() {
        runner.withPropertyValues("buzhou.store.type=jdbc").run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouStores.class);
            assertThat(ctx.getBean(BuzhouStores.class).messageStore().getClass().getSimpleName())
                    .contains("Jdbc");
        });
    }

    @Test
    void notActiveForMemoryType() {
        runner.withPropertyValues("buzhou.store.type=memory")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(BuzhouStores.class));
    }

    @Test
    void recoveryFacilitiesAssembleByDefaultWhenJdbcType() {
        runner.withPropertyValues("buzhou.store.type=jdbc").run(ctx -> {
            assertThat(ctx).hasSingleBean(JdbcBuzhouRecoveryStores.class);
            assertThat(ctx).hasSingleBean(RunRegistry.class);
            assertThat(ctx).hasSingleBean(ToolCallLog.class);
            // 恢复设施与完整组合同源（同一实例派生）
            assertThat(ctx.getBean(RunRegistry.class))
                    .isSameAs(ctx.getBean(JdbcBuzhouRecoveryStores.class).runRegistry());
        });
    }

    @Test
    void recoveryFacilitiesAbsentWhenDisabledByProperty() {
        runner.withPropertyValues(
                        "buzhou.store.type=jdbc",
                        "buzhou.store.jdbc.recovery-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RunRegistry.class);
                    assertThat(ctx).doesNotHaveBean(ToolCallLog.class);
                    // 核心六槽不受开关影响
                    assertThat(ctx).hasSingleBean(BuzhouStores.class);
                });
    }

    @Test
    void observabilityStoreWrappedOnlyWhenDegradePolicyConfigured() {
        // 默认 FAIL_TURN：观测槽保持裸实现（既有原样抛语义）
        runner.withPropertyValues("buzhou.store.type=jdbc").run(ctx ->
                assertThat(ctx.getBean(BuzhouStores.class).observabilityStore().getClass().getSimpleName())
                        .isEqualTo("JdbcObservabilityStore"));
        // DEGRADE：观测槽经降级装饰器包装（ticket 32）
        runner.withPropertyValues(
                        "buzhou.store.type=jdbc",
                        "buzhou.store.write-failure-policy=DEGRADE")
                .run(ctx ->
                        assertThat(ctx.getBean(BuzhouStores.class).observabilityStore().getClass().getSimpleName())
                                .isEqualTo("DegradingObservabilityStore"));
    }
}
