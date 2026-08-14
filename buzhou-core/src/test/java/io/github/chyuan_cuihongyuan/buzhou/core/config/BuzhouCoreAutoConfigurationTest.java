package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.AgentRuntimeLifecycle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
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
}
