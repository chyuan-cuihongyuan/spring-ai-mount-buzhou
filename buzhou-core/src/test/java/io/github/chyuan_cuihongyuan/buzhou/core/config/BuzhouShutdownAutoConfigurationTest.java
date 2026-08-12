package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 优雅停机 SmartLifecycle 装配测试（ticket 04）：drain 生命周期 bean 装配 / 开关 / 超时派生 / 停机触发。
 */
class BuzhouShutdownAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class))
            .withBean(ScriptedChatModel.class, ScriptedChatModel::new);

    @Test
    void drainLifecycleAssembledByDefaultWithHighestPhase() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouDrainLifecycle.class);
            // 相位定值 SmartLifecycle.DEFAULT_PHASE（Boot 4 最高，先于 web 容器与观测管线停止）
            assertThat(ctx.getBean(BuzhouDrainLifecycle.class).getPhase())
                    .isEqualTo(SmartLifecycle.DEFAULT_PHASE);
        });
    }

    @Test
    void drainLifecycleNotAssembledWhenShutdownDisabled() {
        runner.withPropertyValues("buzhou.shutdown.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(BuzhouDrainLifecycle.class);
        });
    }

    @Test
    void drainTimeoutBindsFromBuzhouShutdownProperty() {
        runner.withPropertyValues("buzhou.shutdown.drain-timeout=42s").run(ctx -> {
            assertThat(ctx.getBean(BuzhouDrainLifecycle.class).drainTimeout())
                    .isEqualTo(Duration.ofSeconds(42));
        });
    }

    @Test
    void drainTimeoutDerivesFromSpringLifecyclePropertyWhenBuzhouUnset() {
        runner.withPropertyValues("spring.lifecycle.timeout-per-shutdown-phase=15s").run(ctx -> {
            assertThat(ctx.getBean(BuzhouDrainLifecycle.class).drainTimeout())
                    .isEqualTo(Duration.ofSeconds(15));
        });
    }

    @Test
    void drainTimeoutDefaultsToSpringDefaultWhenBothUnset() {
        // 两者皆无：@Value 默认 30s（Boot 4 spring.lifecycle.timeout-per-shutdown-phase 的规范默认）
        runner.run(ctx -> {
            assertThat(ctx.getBean(BuzhouDrainLifecycle.class).drainTimeout())
                    .isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void lifecycleStopTriggersDrainClosesSessionsAndRefusesSpawnAfter() {
        runner.run(ctx -> {
            AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
            AgentSession session = runtime.spawn("app", "agent", "sid-lifecycle");
            // SmartLifecycle.stop() 触发 drain（与编程式入口同一编排）
            BuzhouDrainLifecycle lifecycle = ctx.getBean(BuzhouDrainLifecycle.class);
            lifecycle.start();
            lifecycle.stop();
            // 会话已 close：后续 chat 抛 IllegalStateException
            assertThatThrownBy(() -> session.chat("x")).isInstanceOf(IllegalStateException.class);
            // drain 后 spawn 拒新
            assertThatThrownBy(() -> runtime.spawn("app", "agent", "after-stop"))
                    .isInstanceOf(RuntimeDrainingException.class);
        });
    }

    @Test
    void repeatedStopIsIdempotent() {
        runner.run(ctx -> {
            AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
            runtime.spawn("app", "agent", "sid-idem-stop");
            BuzhouDrainLifecycle lifecycle = ctx.getBean(BuzhouDrainLifecycle.class);
            lifecycle.start();
            // 重复 stop：复用 DefaultAgentRuntime 的 drain 幂等（首次结果共享），不抛
            lifecycle.stop();
            lifecycle.stop();
            lifecycle.stop();
        });
    }
}
