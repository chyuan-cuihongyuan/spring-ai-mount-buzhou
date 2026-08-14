package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.AgentRuntimeLifecycle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 内核自装配测试（ticket 22）：内存 store 默认装配、AgentRuntime 收集 RuntimeConfig bean 合成。
 * impl-30 / spec 13 §core-1 增补：core SmartLifecycle 装配与上下文 stop 触发。
 */
class BuzhouCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class))
            .withBean(ScriptedChatModel.class, ScriptedChatModel::new);

    @Test
    void memoryStoresAndAgentRuntimeAssembleByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouStores.class);
            assertThat(ctx).hasSingleBean(AgentRuntime.class);
            // spawn 端到端走通装配链（HookChain / ChatClient 构建 / 租约）
            AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
            runtime.spawn("app", "agent", "sid-core-1").close();
        });
    }

    @Test
    void collectsRuntimeConfigBeansIntoAgentRuntime() {
        runner.withBean(RuntimeConfig.class, () -> RuntimeConfig.defaults())
                .run(ctx -> {
                    assertThat(ctx).getBeans(RuntimeConfig.class).hasSize(1);
                    ctx.getBean(AgentRuntime.class).spawn("app", "agent", "sid-core-2").close();
                });
    }

    /** impl-33 / spec 13 §core-3：租约参数默认值（TTL 90s、续租间隔 TTL/3=30s）。 */
    @Test
    void shouldApplyLeaseDefaults_whenPropertiesAbsent() {
        BuzhouCoreProperties properties = new BuzhouCoreProperties(null, null, null, null, null, null);
        assertThat(properties.leaseTtl()).isEqualTo(BuzhouCoreProperties.DEFAULT_LEASE_TTL);
        assertThat(properties.effectiveLeaseRenewInterval())
                .isEqualTo(java.time.Duration.ofSeconds(30));
        // impl-30：停机排空预算默认 30s
        assertThat(properties.lifecycle().timeoutPerShutdownPhase())
                .isEqualTo(BuzhouCoreProperties.Lifecycle.DEFAULT_TIMEOUT_PER_SHUTDOWN_PHASE)
                .isEqualTo(Duration.ofSeconds(30));
        // impl-34：事件分发默认 SYNC（既有内联行为不变）
        assertThat(properties.core().eventDispatch().mode())
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.Mode.SYNC);
        assertThat(properties.core().eventDispatch().capacity())
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.DEFAULT_CAPACITY);
    }

    /** impl-33：buzhou.lease-ttl / buzhou.lease-renew-interval 经 kebab-case 绑定生效。 */
    @Test
    void shouldBindLeaseParameters_whenKebabPropertiesConfigured() {
        runner.withPropertyValues(
                        "buzhou.lease-ttl=5s",
                        "buzhou.lease-renew-interval=1s")
                .run(ctx -> {
                    BuzhouCoreProperties properties = ctx.getBean(BuzhouCoreProperties.class);
                    assertThat(properties.leaseTtl()).isEqualTo(java.time.Duration.ofSeconds(5));
                    assertThat(properties.effectiveLeaseRenewInterval())
                            .isEqualTo(java.time.Duration.ofSeconds(1));
                });
    }

    /** impl-30：buzhou.lifecycle.timeout-per-shutdown-phase 经 kebab-case 绑定生效。 */
    @Test
    void shouldBindShutdownTimeout_whenLifecyclePropertyConfigured() {
        runner.withPropertyValues("buzhou.lifecycle.timeout-per-shutdown-phase=7s")
                .run(ctx -> {
                    BuzhouCoreProperties properties = ctx.getBean(BuzhouCoreProperties.class);
                    assertThat(properties.lifecycle().timeoutPerShutdownPhase())
                            .isEqualTo(Duration.ofSeconds(7));
                });
    }

    /**
     * impl-36 / spec 13 §growth-8：buzhou.store.in-memory.* 经 kebab-case 绑定生效；
     * 默认值 = max-sessions 1000 / per-session 消息 5000 / 观测会话 1000 / 单会话记录 10000。
     */
    @Test
    void shouldBindInMemoryBounds_whenStoreInMemoryConfigured() {
        runner.withPropertyValues(
                        "buzhou.store.in-memory.max-sessions=3",
                        "buzhou.store.in-memory.max-messages-per-session=4",
                        "buzhou.store.in-memory.max-observability-sessions=5",
                        "buzhou.store.in-memory.max-observability-records-per-session=6")
                .run(ctx -> {
                    BuzhouCoreProperties properties = ctx.getBean(BuzhouCoreProperties.class);
                    var inMemory = properties.store().inMemory();
                    assertThat(inMemory.maxSessions()).isEqualTo(3);
                    assertThat(inMemory.maxMessagesPerSession()).isEqualTo(4);
                    assertThat(inMemory.maxObservabilitySessions()).isEqualTo(5);
                    assertThat(inMemory.maxObservabilityRecordsPerSession()).isEqualTo(6);
                    // 装配链路：配置真实流入内存套件（填满 3 个会话后，第 4 个的消息写入即被拒）
                    BuzhouStores stores = ctx.getBean(BuzhouStores.class);
                    for (int i = 1; i <= 3; i++) {
                        stores.messageStore().append("quota-s-" + i, List.of(msg("quota-s-" + i)));
                    }
                    assertThatThrownBy(() -> stores.messageStore().append(
                            "quota-s-4", List.of(msg("quota-s-4"))))
                            .isInstanceOf(QuotaExceededException.class)
                            .hasMessageContaining("maxSessions=3");
                });
    }

    /** impl-36：未配置时内存套件容量配额取默认值。 */
    @Test
    void shouldApplyInMemoryDefaults_whenStoreInMemoryAbsent() {
        runner.run(ctx -> {
            BuzhouCoreProperties properties = ctx.getBean(BuzhouCoreProperties.class);
            var inMemory = properties.store().inMemory().toConfig();
            assertThat(inMemory.maxSessions())
                    .isEqualTo(InMemoryStoreConfig.DEFAULT_MAX_SESSIONS);
            assertThat(inMemory.maxMessagesPerSession())
                    .isEqualTo(InMemoryStoreConfig.DEFAULT_MAX_MESSAGES_PER_SESSION);
        });
    }

    /** impl-30：core lifecycle 装配（phase 最大）且上下文 stop 触发停机序列（回调后拒绝 spawn）。 */
    @Test
    void shouldRegisterCoreLifecycleAndRejectSpawn_whenContextStops() {
        AtomicReference<DefaultAgentRuntime> runtimeRef = new AtomicReference<>();
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AgentRuntimeLifecycle.class);
            AgentRuntimeLifecycle lifecycle = ctx.getBean(AgentRuntimeLifecycle.class);
            assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.CORE);
            assertThat(lifecycle.isRunning()).isTrue(); // refresh 完成即自启动
            runtimeRef.set(ctx.getBean(DefaultAgentRuntime.class));
            runtimeRef.get().spawn("app", "agent", "sid-core-lifecycle").close();
        });
        // ApplicationContextRunner 关闭上下文 → DefaultLifecycleProcessor stop → 停机序列已跑
        DefaultAgentRuntime runtime = runtimeRef.get();
        assertThat(runtime.isShuttingDown()).isTrue();
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "after-stop"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("拒绝创建新会话");
    }

    private static BuzhouMessage msg(String sessionId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                "content", List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** impl-37：sweeper 默认装配自启动；enabled=false 只关调度——bean 恒在可手动触发。 */
    @Test
    void retentionSweeperAssemblesByDefaultAndCanBeDisabled() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper.class);
            var sweeper = ctx.getBean(io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper.class);
            assertThat(sweeper.isAutoStartup()).isTrue(); // SmartLifecycle 自启动
            assertThat(sweeper.sweepOnce()).isNotNull(); // 手动触发可用
        });
        runner.withPropertyValues("buzhou.retention.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(
                            io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper.class);
                    var sweeper = ctx.getBean(
                            io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper.class);
                    assertThat(sweeper.isAutoStartup()).isFalse(); // 不排程
                    assertThat(sweeper.isRunning()).isFalse();
                    assertThat(sweeper.sweepOnce()).isNotNull(); // 各策略仍可手动触发
                });
    }

    /** impl-37：buzhou.retention.* 经 kebab-case 绑定生效。 */
    @Test
    void shouldBindRetentionProperties_whenKebabConfigured() {
        runner.withPropertyValues(
                        "buzhou.retention.sweep-interval=30m",
                        "buzhou.retention.session-retention=5h",
                        "buzhou.retention.observability-ttl=2d",
                        "buzhou.retention.summary-keep-versions=7",
                        "buzhou.retention.run-completed-retention=12h")
                .run(ctx -> {
                    BuzhouRetentionProperties retention = ctx.getBean(BuzhouRetentionProperties.class);
                    assertThat(retention.sweepInterval()).isEqualTo(Duration.ofMinutes(30));
                    assertThat(retention.sessionRetention()).isEqualTo(Duration.ofHours(5));
                    assertThat(retention.observabilityTtl()).isEqualTo(Duration.ofDays(2));
                    assertThat(retention.summaryKeepVersions()).isEqualTo(7);
                    assertThat(retention.runCompletedRetention()).isEqualTo(Duration.ofHours(12));
                });
    }
}
