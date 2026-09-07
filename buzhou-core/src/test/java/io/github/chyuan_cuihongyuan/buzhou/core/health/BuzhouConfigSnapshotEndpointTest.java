package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 343 / impl-366：生效配置端点回归——yml+env 生效值 / 密钥掩码 /
 * 前缀过滤 / 空 map / 键有序 / 装配。
 */
class BuzhouConfigSnapshotEndpointTest {

    private BuzhouConfigSnapshotEndpoint endpoint(MockEnvironment environment) {
        return new BuzhouConfigSnapshotEndpoint(environment);
    }

    @Test
    void effectiveValuesFromEnvOverridesAreShown() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("buzhou.token-budget.warning-percent", "80")
                .withProperty("buzhou.backpressure.error-budget-freeze.enabled", "true")
                .withProperty("spring.application.name", "other-prefix-app");
        Map<String, String> snapshot = endpoint(environment).buzhouConfig();
        assertThat(snapshot).containsEntry("buzhou.token-budget.warning-percent", "80")
                .containsEntry("buzhou.backpressure.error-budget-freeze.enabled", "true");
        assertThat(snapshot.keySet()).allSatisfy(key -> assertThat(key).startsWith("buzhou."));
        assertThat(snapshot).doesNotContainKey("spring.application.name"); // 前缀过滤
    }

    @Test
    void sensitiveValuesMasked_generously() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("buzhou.security.message-encryption.master-key", "SUPERSECRET==")
                .withProperty("buzhou.security.message-encryption.previous-master-key", "OLD==")
                .withProperty("buzhou.tools.baggage.tenant", "acme");
        Map<String, String> snapshot = endpoint(environment).buzhouConfig();
        assertThat(snapshot).hasSize(3);
        assertThat(snapshot).containsEntry("buzhou.tools.baggage.tenant", "acme"); // 非敏感明文
        assertThat(snapshot).doesNotContainValue("SUPERSECRET==");
        assertThat(snapshot.get("buzhou.security.message-encryption.master-key"))
                .isEqualTo(BuzhouConfigSnapshotEndpoint.MASK);
    }

    @Test
    void emptyEnvironmentYieldsEmptyMap_orderedKeys() {
        Map<String, String> snapshot = endpoint(new MockEnvironment()).buzhouConfig();
        assertThat(snapshot).isEmpty(); // 无属性诚实空
        MockEnvironment environment = new MockEnvironment()
                .withProperty("buzhou.bbb", "1")
                .withProperty("buzhou.aaa", "2");
        Map<String, String> ordered = endpoint(environment).buzhouConfig();
        assertThat(ordered.keySet()).containsExactly("buzhou.aaa", "buzhou.bbb"); // 键有序稳定
    }

    @Test
    void maskMatchingUnitCases() {
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.security.message-encryption.master-key", "v")).isEqualTo("***");
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.webhook.secret", "v")).isEqualTo("***");
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.some.password", "v")).isEqualTo("***");
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.api-token", "v")).isEqualTo("***");
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.credential", "v")).isEqualTo("***");
        assertThat(BuzhouConfigSnapshotEndpoint.maskIfNeeded(
                "buzhou.tools.result-limit-chars", "20000")).isEqualTo("20000");
    }

    @Test
    void assemblesUnderActuator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(BuzhouConfigSnapshotEndpoint.class);
                });
    }
}
