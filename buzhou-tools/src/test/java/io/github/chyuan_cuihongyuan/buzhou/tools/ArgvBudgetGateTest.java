package io.github.chyuan_cuihongyuan.buzhou.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1845 / T2892：argv 双闸——总量/单参/先序/边界。 */
class ArgvBudgetGateTest {

    /** 字节账：每参字符数+1（NUL 分隔）。 */
    @Test
    void shouldCountBytesWithNulSeparator() {
        assertThat(ArgvBudgetGate.totalBytes(List.of("ls", "-la")))
                .isEqualTo(3L + 4L);
        assertThat(ArgvBudgetGate.totalBytes(List.of())).isZero();
        assertThat(ArgvBudgetGate.totalBytes(null)).isZero();
    }

    /** 三态判定：单参先于总量；边界含上（== 均 FIT）。 */
    @Test
    void shouldJudgeThreeVerdictsWithSingleFirst() {
        assertThat(ArgvBudgetGate.verify(List.of("run", "--flag")))
                .isEqualTo(ArgvBudgetGate.Verdict.FIT);
        // 单参超限（131073 > 131072）即使总量也超——单参先判
        assertThat(ArgvBudgetGate.verify(List.of("x".repeat(131_073))))
                .isEqualTo(ArgvBudgetGate.Verdict.OVER_SINGLE_ARG);
        // 总量超限（每参 1 字节 + NUL = 2B，101 参 = 202B > 200）
        assertThat(ArgvBudgetGate.verify(
                IntStream.range(0, 101).mapToObj(i -> "x").toList(), 200, 1000))
                .isEqualTo(ArgvBudgetGate.Verdict.OVER_TOTAL);
        // 边界含上：单参 == 上限 FIT、总量 == 预算 FIT
        assertThat(ArgvBudgetGate.verify(List.of("y".repeat(131_071)), 1_048_576, 131_072))
                .isEqualTo(ArgvBudgetGate.Verdict.FIT);
        assertThat(ArgvBudgetGate.verify(List.of("ab", "cd"), 6, 100))
                .isEqualTo(ArgvBudgetGate.Verdict.FIT);
    }

    /** 畸形入参 fail-fast：负预算、零单参上限、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ArgvBudgetGate.verify(List.of(), -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法闸参");
        assertThatThrownBy(() -> ArgvBudgetGate.verify(List.of(), 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArgvBudgetGate.verify(
                java.util.Arrays.asList("ok", null), 100, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参数不能为 null");
    }
}
