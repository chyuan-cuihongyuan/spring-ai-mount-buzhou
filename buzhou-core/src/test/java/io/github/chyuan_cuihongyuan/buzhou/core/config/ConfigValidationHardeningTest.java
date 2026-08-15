package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 43 §B / T158 / impl-129：配置校验补全——runaway/backpressure 全键 fail-fast（越界值
 * 不再静默派生）；webhook maxAttempts/outboxCapacity 非法值由静默回退默认改为启动即拒。
 */
class ConfigValidationHardeningTest {

    @Test
    void runawayBoundsValidatedAtConstruction() {
        // 合法全量先过（null = 不限的宽容保留）
        assertThatCode(() -> new BuzhouRunawayProperties(null,
                new BuzhouRunawayProperties.PerTurn(50, 200, Duration.ofMinutes(10)),
                new BuzhouRunawayProperties.PerSession(1000, 5000),
                Map.of("expensive_*", new BuzhouRunawayProperties.PerToolLimit(3)),
                0.5,
                new BuzhouRunawayProperties.Repetition(3, "flag-only"),
                "emit-event")).doesNotThrowAnyException();

        assertThatThrownBy(() -> new BuzhouRunawayProperties(null,
                new BuzhouRunawayProperties.PerTurn(-5, null, null), null, null, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("per-turn.max-steps").hasMessageContaining("-5");
        assertThatThrownBy(() -> new BuzhouRunawayProperties(null,
                null, null, null, 1.5, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("soft-threshold-ratio");
        assertThatThrownBy(() -> new BuzhouRunawayProperties(null,
                null, null, Map.of("x", new BuzhouRunawayProperties.PerToolLimit(0)),
                null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("per-tool[x].max-calls");
        assertThatThrownBy(() -> new BuzhouRunawayProperties(null,
                null, null, null, null,
                new BuzhouRunawayProperties.Repetition(1, null), null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("repetition.consecutive");
    }

    @Test
    void backpressureBoundsAndPoliciesValidatedAtConstruction() {
        assertThatCode(() -> new BuzhouBackpressureProperties(null,
                64, Duration.ofSeconds(5), "FAIL_FAST",
                new BuzhouBackpressureProperties.Tool(4, Duration.ofSeconds(30),
                        Duration.ZERO, "QUEUE"))).doesNotThrowAnyException();

        assertThatThrownBy(() -> new BuzhouBackpressureProperties(null,
                0, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("max-concurrent-sessions");
        // 非法策略值此前被静默归 QUEUE——现在启动即拒
        assertThatThrownBy(() -> new BuzhouBackpressureProperties(null,
                null, null, "fail-fast", null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("spawn-overload-policy").hasMessageContaining("QUEUE");
        assertThatThrownBy(() -> new BuzhouBackpressureProperties(null,
                null, null, null,
                new BuzhouBackpressureProperties.Tool(null, null,
                        Duration.ofMillis(-1), null)))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("permit-acquire-timeout");
        // 语义回归：合法值解析不受校验影响
        assertThat(new BuzhouBackpressureProperties(null, null, null, null, null)
                .effectiveSpawnOverloadPolicy())
                .isEqualTo(OverloadPolicy.QUEUE);
    }

    @Test
    void webhookIllegalValuesRejectedInsteadOfSilentDefault() {
        // null 仍取默认（宽容只留给未配置）
        assertThat(new BuzhouWebhookProperties("http://x", null, null, null, null, null)
                .maxAttempts()).isEqualTo(8);
        // 非法值静默回退 → 启动即拒（pre-1.0 破坏性变更）
        assertThatThrownBy(() -> new BuzhouWebhookProperties("http://x", null, null, 0, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("max-attempts");
        assertThatThrownBy(() -> new BuzhouWebhookProperties("http://x", null, null, null, -1, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("outbox-capacity");
    }
}
