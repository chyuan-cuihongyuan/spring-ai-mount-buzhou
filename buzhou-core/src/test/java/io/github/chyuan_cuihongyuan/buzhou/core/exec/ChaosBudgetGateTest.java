package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1822 / T2846：混沌预算门——窗口优先于预算、使用账钳零。 */
class ChaosBudgetGateTest {

    /** 窗口优先：不在窗即禁（预算再多不跑）；窗内零预算耗尽。 */
    @Test
    void windowTakesPrecedenceOverBudget() {
        ChaosBudgetGate.Window window = new ChaosBudgetGate.Window(1000L, 2000L);
        assertThat(ChaosBudgetGate.decide(999L, 500L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.FORBIDDEN_WINDOW);
        assertThat(ChaosBudgetGate.decide(999L, 2500L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.FORBIDDEN_WINDOW);
        assertThat(ChaosBudgetGate.decide(999L, 1500L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.MAY_RUN);
        assertThat(ChaosBudgetGate.decide(0, 1500L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.OUT_OF_BUDGET);
    }

    /** 窗口边界含两端；空窗口永不放行。 */
    @Test
    void windowBoundariesAndEmptyWindow() {
        ChaosBudgetGate.Window window = new ChaosBudgetGate.Window(1000L, 2000L);
        assertThat(ChaosBudgetGate.decide(1L, 1000L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.MAY_RUN);
        assertThat(ChaosBudgetGate.decide(1L, 2000L, window))
                .isEqualTo(ChaosBudgetGate.Verdict.MAY_RUN);

        ChaosBudgetGate.Window empty = new ChaosBudgetGate.Window(1000L, 1000L);
        assertThat(ChaosBudgetGate.decide(1L, 1000L, empty))
                .isEqualTo(ChaosBudgetGate.Verdict.MAY_RUN);
        assertThat(ChaosBudgetGate.decide(1L, 999L, empty))
                .isEqualTo(ChaosBudgetGate.Verdict.FORBIDDEN_WINDOW);
    }

    /** 使用账：累计+钳零+烧尽比；超支照实入账不外泄负值。 */
    @Test
    void usageAccountsAndClamps() {
        ChaosBudgetGate.Usage usage = ChaosBudgetGate.usage(100L, List.of(30L, 40L, 50L));
        assertThat(usage.spentMillis()).isEqualTo(120L);
        assertThat(usage.remainingMillis()).isZero();
        assertThat(usage.exhausted()).isTrue();
        assertThat(usage.burnRatio()).isEqualTo(1.2d);

        ChaosBudgetGate.Usage within = ChaosBudgetGate.usage(100L, List.of(30L));
        assertThat(within.remainingMillis()).isEqualTo(70L);
        assertThat(within.burnRatio()).isEqualTo(0.3d);

        assertThat(ChaosBudgetGate.usage(0, null).burnRatio()).isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负预算、倒挂窗口、null/负实验花费。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ChaosBudgetGate.decide(-1, 0,
                new ChaosBudgetGate.Window(0, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("budgetRemainingMillis 不能为负");
        assertThatThrownBy(() -> new ChaosBudgetGate.Window(2, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法窗口");
        assertThatThrownBy(() -> ChaosBudgetGate.usage(10, List.of(1L, -1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChaosBudgetGate.usage(10,
                java.util.Arrays.asList(1L, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
