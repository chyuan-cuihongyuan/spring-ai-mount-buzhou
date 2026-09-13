package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BuzhouResilienceHealthAutoConfiguration 直测（spec 1200 / T1801 / K 会话 R1 补测——
 * 内部类 ResilienceHealth 此前零覆盖）。
 *
 * <p>同包直接 new 健康委托（McpHealthHintsTest 先例）断三态语义；runner 补装配面。
 * 注：resilience 模块无 spring-boot-health 依赖（健康面经可选依赖在部署侧生效），
 * indicator 内部类不在本模块测试 classpath 激活，不对其断言。
 */
class BuzhouResilienceHealthAutoConfigurationTest {

    private static final String ENABLED_KEY = "buzhou.resilience.enabled";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouResilienceHealthAutoConfiguration.class));

    @Test
    void disabledHealthIsUnknownWithDisabledDetail() {
        BuzhouResilienceHealthAutoConfiguration.ResilienceHealth health =
                new BuzhouResilienceHealthAutoConfiguration.ResilienceHealth(false, null);
        assertThat(health.mechanism()).isEqualTo("resilience");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsExactlyEntriesOf(Map.of("enabled", false));
    }

    @Test
    void enabledHealthIsUpWithStatsSnapshot() {
        ResilienceStats stats = new ResilienceStats();
        stats.recordRetryAttempt();
        BuzhouResilienceHealthAutoConfiguration.ResilienceHealth health =
                new BuzhouResilienceHealthAutoConfiguration.ResilienceHealth(true, stats);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        // details 透传 stats 快照（计数快照是启用态的详情面）
        assertThat(health.details()).containsEntry("retryAttempts", 1L);
    }

    @Test
    void enabledWithoutStatsReportsEnabledOnly() {
        BuzhouResilienceHealthAutoConfiguration.ResilienceHealth health =
                new BuzhouResilienceHealthAutoConfiguration.ResilienceHealth(true, null);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsExactlyEntriesOf(Map.of("enabled", true));
    }

    @Test
    void runnerAssemblesHealthBeanAndHonorsSwitch() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(
                BuzhouResilienceHealthAutoConfiguration.ResilienceHealth.class));
        runner.withPropertyValues(ENABLED_KEY + "=false").run(ctx -> {
            BuzhouResilienceHealthAutoConfiguration.ResilienceHealth health =
                    ctx.getBean(BuzhouResilienceHealthAutoConfiguration.ResilienceHealth.class);
            assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        });
    }
}
