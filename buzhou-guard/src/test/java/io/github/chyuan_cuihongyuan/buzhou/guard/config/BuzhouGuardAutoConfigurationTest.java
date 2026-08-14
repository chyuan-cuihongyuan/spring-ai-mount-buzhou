package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditTrailCollector;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.InMemoryAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.JdbcAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouGuardAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouGuardAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GuardModule.class);
            assertThat(ctx).hasSingleBean(GuardAuthApi.class);
        });
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.guard.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GuardModule.class));
    }

    /**
     * impl-30 / spec 13 §core-1：guard lifecycle 装配——phase 声明（core/memory/spill
     * 之后、store 之前停）；impl-39：审计链接线后 lifecycle 持有终局自检引用。
     */
    @Test
    void shouldRegisterGuardLifecyclePlaceholder_whenEnabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GuardModuleLifecycle.class);
            GuardModuleLifecycle lifecycle = ctx.getBean(GuardModuleLifecycle.class);
            assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.GUARD);
            assertThat(lifecycle.getPhase()).isLessThan(BuzhouLifecyclePhases.SPILL);
            assertThat(lifecycle.getPhase()).isGreaterThan(BuzhouLifecyclePhases.STORE);
            assertThat(lifecycle.isRunning()).isTrue();
        });
    }

    /**
     * impl-39 / spec 13 §T64：审计默认在线（随 guard 开）——无密钥时降级纯哈希链
     * （SigningKeyRing.hasSigningKey=false），无 DataSource 时 InMemory 有界环形。
     */
    @Test
    void auditWiredByDefaultAndDegradesWithoutKeys() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AuditRecordStore.class);
            assertThat(ctx).hasSingleBean(SigningKeyRing.class);
            assertThat(ctx).hasSingleBean(AuditChain.class);
            assertThat(ctx).hasSingleBean(AuditTrailCollector.class);
            assertThat(ctx.getBean(AuditRecordStore.class))
                    .isInstanceOf(InMemoryAuditRecordStore.class);
            assertThat(ctx.getBean(SigningKeyRing.class).hasSigningKey()).isFalse();
        });
    }

    @Test
    void auditDisabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.guard.audit.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(AuditRecordStore.class);
                    assertThat(ctx).doesNotHaveBean(AuditChain.class);
                    assertThat(ctx).doesNotHaveBean(AuditTrailCollector.class);
                    // guard 本体不受影响
                    assertThat(ctx).hasSingleBean(GuardModule.class);
                });
    }

    /** auto=有 DataSource 即 JDBC append-only；JDBC 续链追加落表、链可验。 */
    @Test
    void auditStoreJdbcWhenDataSourcePresent() {
        runner.withBean(javax.sql.DataSource.class, () ->
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:audit-ds-" + java.util.UUID.randomUUID()
                                        + ";DB_CLOSE_DELAY=-1", "sa", ""))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AuditRecordStore.class);
                    assertThat(ctx.getBean(AuditRecordStore.class))
                            .isInstanceOf(JdbcAuditRecordStore.class);
                    AuditChain chain = ctx.getBean(AuditChain.class);
                    AuditTrailCollector collector = ctx.getBean(AuditTrailCollector.class);
                    collector.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                            "guard.tool.blocked",
                            java.util.Map.of("sessionId", "s1")));
                    assertThat(ctx.getBean(AuditRecordStore.class).count()).isEqualTo(1);
                    assertThat(chain.verify(ctx.getBean(SigningKeyRing.class))).isTrue();
                });
    }

    /** impl-40 / spec 13 §T64：策略门默认关；显式开启后热加载引擎 + PolicyGateHook 进装配。 */
    @Test
    void policyGateDisabledByDefaultAndWiredWhenEnabled() {
        runner.run(ctx ->
                assertThat(ctx).doesNotHaveBean(io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher.class));

        runner.withPropertyValues("buzhou.guard.policy.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(
                    io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher.class);
            GuardModule module = ctx.getBean(GuardModule.class);
            assertThat(module.configure().hooks())
                    .anyMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyGateHook);
            // 测试 classpath 策略生效：deny-others 规则可用（默认拒兜底存在）
            io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher refresher =
                    ctx.getBean(io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher.class);
            assertThat(refresher.ruleCount()).isEqualTo(2);
            ctx.stop(); // 关闭轮询线程
        });
    }

    @Test
    void auditStoreInMemoryForcedEvenWithDataSource() {
        runner.withPropertyValues("buzhou.guard.audit.store=in-memory")
                .withBean(javax.sql.DataSource.class, () ->
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:audit-mem-" + java.util.UUID.randomUUID()
                                        + ";DB_CLOSE_DELAY=-1", "sa", ""))
                .run(ctx -> assertThat(ctx.getBean(AuditRecordStore.class))
                        .isInstanceOf(InMemoryAuditRecordStore.class));
    }
}
