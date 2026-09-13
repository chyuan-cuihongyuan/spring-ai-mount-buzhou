package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-655 / spec 902：pass@k 无偏估计——边界值、论文已知值（HumanEval §2.1
 * 例：n=4,c=2,k=2 → 1−C(2,2)/C(4,2)=5/6）、k=n 全通过恒 1、k=1 线性 c/n、
 * 聚合平均、参数校验。
 */
class EvalPassAtKTest {

    @Test
    void boundaryCases() {
        assertThat(EvalPassAtK.estimate(5, 0, 3)).isZero(); // 全失败 → 0
        assertThat(EvalPassAtK.estimate(5, 5, 3)).isEqualTo(1.0); // 全通过 → 1
        assertThat(EvalPassAtK.estimate(5, 2, 1)).isEqualTo(0.4); // k=1 线性 c/n
        assertThat(EvalPassAtK.estimate(5, 2, 5)).isEqualTo(1.0); // k=n：有通过必被抽到
    }

    @Test
    void paperKnownValue() {
        // HumanEval 论文例：n=4, c=2, k=2 → 1 − C(2,2)/C(4,2) = 1 − 1/6 = 5/6
        assertThat(EvalPassAtK.estimate(4, 2, 2)).isCloseTo(5.0 / 6.0, within(1e-12));
    }

    @Test
    void numericallyStableForLargerN() {
        // 连乘形式在大 n 下仍稳定（组合数阶乘实现会溢出/精度崩坏）
        double v = EvalPassAtK.estimate(200, 100, 50);
        assertThat(v).isBetween(0.0, 1.0);
        // 单调性：k 越大 pass@k 越大（同 n,c）
        assertThat(EvalPassAtK.estimate(50, 10, 5))
                .isLessThanOrEqualTo(EvalPassAtK.estimate(50, 10, 20) + 1e-12);
    }

    @Test
    void aggregateAveragesPerItemEstimates() {
        // 两项：一项 c=0（pass@2=0）、一项 n=4,c=2（pass@2=5/6）→ 平均 = 5/12
        double avg = EvalPassAtK.aggregate(new int[]{0, 2}, 4, 2);
        assertThat(avg).isCloseTo(5.0 / 12.0, within(1e-12));
        assertThat(EvalPassAtK.aggregate(new int[0], 4, 2)).isZero(); // 空集约定 0
    }

    @Test
    void argsValidated() {
        assertThatThrownBy(() -> EvalPassAtK.estimate(0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalPassAtK.estimate(4, 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalPassAtK.estimate(4, 2, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvalPassAtK.estimate(4, 2, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
