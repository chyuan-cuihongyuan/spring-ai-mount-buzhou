package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolHealthProber;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreakerHook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 305/306 / impl-328/329：工具健康探测装配 + 工具熔断 yml 装配回归——
 * 默认关零 bean / enabled 装配 / DOWN 详情严格口径 / 参数绑定与非法拒绝。
 */
class ToolHealthAndCircuitAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    // ---- spec 305：健康探测装配 ----

    @Test
    void healthDisabledByDefaultRegistersNothing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(ToolHealthProber.class);
            assertThat(context.containsBean("buzhouToolHealth"))
                    .as("默认关：探测健康面不装配").isFalse();
        });
    }

    @Test
    void healthEnabledAssemblesProberAndHealthFace() {
        runner.withPropertyValues("buzhou.tools.health.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(ToolHealthProber.class);
            assertThat(context).hasBean("buzhouToolHealth");
            // 容器关闭（destroyMethod=stop）不停调度则线程泄漏——ContextRunner 自动 close 覆盖
            BuzhouHealth health = context.getBean("buzhouToolHealth", BuzhouHealth.class);
            assertThat(health.mechanism()).isEqualTo("tools");
            assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
            assertThat(health.details()).containsEntry("registered", 0);
        });
    }

    @Test
    void downToolSurfacesInDetails_mechanismStaysUp() {
        runner.withPropertyValues("buzhou.tools.health.enabled=true").run(context -> {
            ToolHealthProber prober = context.getBean(ToolHealthProber.class);
            prober.register("flaky", () -> false);
            prober.probeOnce();
            BuzhouHealth health = context.getBean("buzhouToolHealth", BuzhouHealth.class);
            assertThat(health.status())
                    .as("严格口径：外部工具 DOWN 不拉低工具机制整体").isEqualTo(BuzhouHealth.Status.UP);
            assertThat(health.details()).containsEntry("down", java.util.List.of("flaky"));
            // lastKnown 不触发新探测（consecutiveDown 不因读取增长）
            assertThat(prober.lastKnown().get("flaky").consecutiveDown()).isEqualTo(1);
        });
    }

    // ---- spec 306：工具熔断 yml 装配 ----

    @Test
    void circuitDisabledByDefaultRegistersNoHook() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(ToolCircuitBreakerHook.class));
    }

    @Test
    void circuitEnabledAssemblesHookAsBuzhouHook() {
        runner.withPropertyValues("buzhou.tools.circuit.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(ToolCircuitBreakerHook.class);
            assertThat(context.getBean(ToolCircuitBreakerHook.class))
                    .as("经 List<BuzhouHook> 自动收集面并入 RuntimeConfig")
                    .isInstanceOf(BuzhouHook.class);
        });
    }

    @Test
    void circuitParamsBind() {
        runner.withPropertyValues(
                "buzhou.tools.circuit.enabled=true",
                "buzhou.tools.circuit.window-size=10",
                "buzhou.tools.circuit.failure-rate-threshold-percent=25.5",
                "buzhou.tools.circuit.cooldown=5s",
                "buzhou.tools.circuit.half-open-trials=1").run(context -> {
            BuzhouToolsProperties props = context.getBean(BuzhouToolsProperties.class);
            assertThat(props.circuit().windowSize()).isEqualTo(10);
            assertThat(props.circuit().failureRateThresholdPercent()).isEqualTo(25.5);
            assertThat(props.circuit().cooldown()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.circuit().halfOpenTrials()).isEqualTo(1);
        });
    }

    @Test
    void circuitRejectsInvalidParams() {
        runner.withPropertyValues(
                "buzhou.tools.circuit.enabled=true",
                "buzhou.tools.circuit.window-size=1").run(context ->
                assertThat(context).hasFailed());
    }
}
