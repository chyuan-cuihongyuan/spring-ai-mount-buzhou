package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PermutationTestFileTest {

    @Test
    void shouldAnchorExactSmallCase() {
        // 全排列小样本锚：{1,2,3} vs {4,5}——所有 C(5,2)=10 种分组中 |差|≥观测 的比例可手算
        // 分组 {x,y}（从 1..5 取 2 个为 A）：A 均值−B 均值：观测 |(1+2)/2−(3+4+5)/3|=|1.5−4|=2.5
        // 枚举：{1,2}:2.5 与镜像 {4,5}:2.5 两分组 ≥2.5 → 精确 2/10=0.2
        // 蒙特卡洛 + 观测自身计入：p 趋 0.2
        double p = PermutationTest.twoSidedPValue(
                new double[]{1, 2}, new double[]{3, 4, 5}, 20000, new Random(3));
        assertThat(p).isCloseTo(0.2, within(0.02));
        // 完全同分布双组：p 应大（无差异不显著）
        double same = PermutationTest.twoSidedPValue(
                new double[]{1, 2, 3, 4, 5}, new double[]{2, 3, 4, 5, 6}, 5000, new Random(4));
        assertThat(same).isGreaterThan(0.3);
    }

    @Test
    void shouldDetectStrongShift() {
        // 强移位圣像：A~N(0,1)×30 vs B~N(3,1)×30——p 应极小
        Random random = new Random(17);
        double[] a = new double[30];
        double[] b = new double[30];
        for (int i = 0; i < 30; i++) {
            a[i] = random.nextGaussian();
            b[i] = 3 + random.nextGaussian();
        }
        assertThat(PermutationTest.twoSidedPValue(a, b, 10000, new Random(5))).isLessThan(0.001);
        // 无移位对照：A vs A'（同分布）——p 均匀偏大
        double[] a2 = new double[30];
        for (int i = 0; i < 30; i++) {
            a2[i] = random.nextGaussian();
        }
        assertThat(PermutationTest.twoSidedPValue(a, a2, 10000, new Random(6))).isGreaterThan(0.01);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {2.0, 3.5};
        assertThat(PermutationTest.twoSidedPValue(a, b, 500, new Random(9)))
                .isEqualTo(PermutationTest.twoSidedPValue(a, b, 500, new Random(9)));
        assertThatThrownBy(() -> PermutationTest.twoSidedPValue(null, b, 100, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermutationTest.twoSidedPValue(new double[]{}, b, 100, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermutationTest.twoSidedPValue(a, b, 0, new Random()))
                .hasMessageContaining("置换次数");
    }
}
