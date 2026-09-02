package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 324 / impl-347：工具金丝雀回归——权重分流确定性/边界 0·100/同名校验/
 * 劣化一次性粘性回滚/未达样本不回滚/同错误率不回滚/View 与构造校验。
 */
class CanaryToolCallbackTest {

    private static final DoubleSupplier ALWAYS = () -> 0.0;
    private static final DoubleSupplier NEVER = () -> 0.9999;

    private static ToolCallback tool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    @Test
    void weightSplitsDeterministically() {
        CanaryToolCallback allCanary = CanaryToolCallback.wrap(
                tool("w", "stable-ok"), tool("w", "canary-ok"), 50, 10, 10, ALWAYS);
        assertThat(allCanary.call("{}")).contains("canary"); // 0 < 50 → canary

        CanaryToolCallback allStable = CanaryToolCallback.wrap(
                tool("w", "stable-ok"), tool("w", "canary-ok"), 50, 10, 10, NEVER);
        assertThat(allStable.call("{}")).contains("stable"); // 99.99 ≥ 50 → stable
    }

    @Test
    void zeroAndHundredBoundaries() {
        CanaryToolCallback zero = CanaryToolCallback.wrap(
                tool("w", "stable-ok"), tool("w", "canary-ok"), 0, 10, 10, ALWAYS);
        assertThat(zero.call("{}")).contains("stable"); // 0% 全稳定

        CanaryToolCallback hundred = CanaryToolCallback.wrap(
                tool("w", "stable-ok"), tool("w", "canary-ok"), 100, 10, 10, NEVER);
        assertThat(hundred.call("{}")).contains("canary"); // 100% 全金丝雀
    }

    @Test
    void nameMismatchRejected() {
        assertThatThrownBy(() -> CanaryToolCallback.wrap(
                tool("weather", "ok"), tool("weather_v2", "ok"), 10, 10, 10, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同名工具");
    }

    @Test
    void degradationTriggersStickyRollback() {
        CanaryToolCallback canary = CanaryToolCallback.wrap(
                tool("w", "stable-ok"),
                tool("w", ToolFeedbackType.EXECUTION_FAILURE.marker() + "\n原因：新版坏"),
                100, 3, 10, ALWAYS);
        canary.call("{}");
        canary.call("{}");
        assertThat(canary.rolledBack())
                .as("样本 2 < minSamples 3——不回滚").isFalse();
        canary.call("{}"); // 第 3 败：100% vs 0% 差 100pp ≥ 10pp
        assertThat(canary.rolledBack()).isTrue();
        assertThat(canary.call("{}"))
                .as("回滚粘性——此后全走 stable（weight 100 也不上）").contains("stable");
        CanaryToolCallback.View view = canary.view();
        assertThat(view.canaryCalls()).isEqualTo(3);
        assertThat(view.canaryErrors()).isEqualTo(3);
        assertThat(view.stableCalls()).isEqualTo(1);
        assertThat(view.stableErrors()).isZero();
    }

    @Test
    void underSamplesNoRollback() {
        CanaryToolCallback canary = CanaryToolCallback.wrap(
                tool("w", "stable-ok"),
                tool("w", ToolFeedbackType.EXECUTION_FAILURE.marker()),
                100, 5, 10, ALWAYS);
        for (int i = 0; i < 4; i++) {
            canary.call("{}");
        }
        assertThat(canary.rolledBack()).isFalse(); // 4 < 5
        assertThat(canary.call("{}")).contains("工具执行失败"); // 仍真跑金丝雀
    }

    @Test
    void sameErrorRateNoRollback() {
        java.util.concurrent.atomic.AtomicBoolean flip =
                new java.util.concurrent.atomic.AtomicBoolean();
        DoubleSupplier alternating = () -> flip.getAndSet(!flip.get()) ? 0.9 : 0.0; // 稳定/金丝雀交替
        CanaryToolCallback canary = CanaryToolCallback.wrap(
                tool("w", ToolFeedbackType.EXECUTION_FAILURE.marker()),
                tool("w", ToolFeedbackType.EXECUTION_FAILURE.marker()),
                50, 3, 10, alternating);
        for (int i = 0; i < 6; i++) {
            canary.call("{}");
        }
        CanaryToolCallback.View view = canary.view();
        assertThat(view.stableCalls()).isEqualTo(3);
        assertThat(view.canaryCalls()).isEqualTo(3);
        assertThat(canary.rolledBack())
                .as("两臂同劣化——差 0pp < 10pp 不回滚（金丝雀不比稳定版差）")
                .isFalse();
    }

    @Test
    void viewStartsCleanAndConstructorValidates() {
        CanaryToolCallback canary = CanaryToolCallback.wrap(
                tool("w", "ok"), tool("w", "ok"), 10, 10, 10, ALWAYS);
        CanaryToolCallback.View view = canary.view();
        assertThat(view.weightPercent()).isEqualTo(10);
        assertThat(view.stableCalls()).isZero();
        assertThat(view.canaryCalls()).isZero();
        assertThat(view.rolledBack()).isFalse();

        assertThatThrownBy(() -> CanaryToolCallback.wrap(
                tool("w", "ok"), tool("w", "ok"), 101, 10, 10, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CanaryToolCallback.wrap(
                tool("w", "ok"), tool("w", "ok"), 10, 0, 10, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CanaryToolCallback.wrap(
                tool("w", "ok"), tool("w", "ok"), 10, 10, -1, ALWAYS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CanaryToolCallback.wrap(
                tool("w", "ok"), tool("w", "ok"), 10, 10, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
