package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内核自装配测试（ticket 22）：内存 store 默认装配、AgentRuntime 收集 RuntimeConfig bean 合成。
 * 背压配置绑定（spec「背压与多层限流 · 05」）。
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

    // ---- 背压配置绑定（spec「背压与多层限流 · 05」） ----

    @Test
    void backpressurePropertiesBindWithDefaultsNull() {
        runner.run(ctx -> {
            BuzhouBackpressureProperties props = ctx.getBean(BuzhouBackpressureProperties.class);
            assertThat(props.enabled()).isTrue();  // safe-by-default
            assertThat(props.maxConcurrentSessions()).isNull();  // null = 不限
            assertThat(props.tool()).isNull();
        });
    }

    @Test
    void backpressureYmlOverridesBindToProperties() {
        runner.withPropertyValues(
                "buzhou.backpressure.max-concurrent-sessions=10",
                "buzhou.backpressure.spawn-queue-timeout=5s",
                "buzhou.backpressure.spawn-overload-policy=FAIL_FAST",
                "buzhou.backpressure.tool.max-concurrent-per-turn=4",
                "buzhou.backpressure.tool.tool-timeout=30s",
                "buzhou.backpressure.tool.permit-acquire-timeout=2s",
                "buzhou.backpressure.tool.overload-policy=FAIL_FAST"
        ).run(ctx -> {
            BuzhouBackpressureProperties props = ctx.getBean(BuzhouBackpressureProperties.class);
            assertThat(props.maxConcurrentSessions()).isEqualTo(10);
            assertThat(props.spawnQueueTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.effectiveSpawnOverloadPolicy())
                    .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.FAIL_FAST);
            assertThat(props.tool().maxConcurrentPerTurn()).isEqualTo(4);
            assertThat(props.tool().toolTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.tool().permitAcquireTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(props.effectiveToolOverloadPolicy())
                    .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.FAIL_FAST);
        });
    }

    @Test
    void backpressureDisabledDropsSpawnGate() {
        runner.withPropertyValues("buzhou.backpressure.enabled=false").run(ctx -> {
            BuzhouBackpressureProperties props = ctx.getBean(BuzhouBackpressureProperties.class);
            assertThat(props.enabled()).isFalse();
            // runtime 仍可 spawn（背压关闭 = 不限）
            ctx.getBean(AgentRuntime.class).spawn("app", "agent", "sid-bp-off").close();
        });
    }
}
