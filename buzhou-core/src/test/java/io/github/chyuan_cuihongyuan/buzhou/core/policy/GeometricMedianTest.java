package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeometricMedianTest {

    @Test
    void shouldMatchSymmetricAnchors() {
        // 对称双点：中点即几何中位数（距离和 2·1=2 唯一最小谷）
        double[] mid = GeometricMedian.median(new double[][]{{-1, 0}, {1, 0}}, 100);
        assertThat(mid[0]).isCloseTo(0.0, within(1e-9));
        assertThat(mid[1]).isCloseTo(0.0, within(1e-9));
        // 等边三角形三点：中心（到三点等距点）
        double[][] triangle = {{0, 0}, {4, 0}, {2, 2 * Math.sqrt(3)}};
        double[] center = GeometricMedian.median(triangle, 200);
        assertThat(center[0]).isCloseTo(2.0, within(1e-6));
        assertThat(center[1]).isCloseTo(2.0 * Math.sqrt(3) / 3.0, within(1e-6));
        // 单点：自身
        assertThat(GeometricMedian.median(new double[][]{{5, -3}}, 1)).containsExactly(5.0, -3.0);
    }

    @Test
    void shouldBeatCentroidOnOutliers() {
        // 离群敏感圣像：簇 {0..0}×9 + 远离群 {100,0}——几何中位数距离和 < 质心距离和
        double[][] points = new double[10][];
        for (int i = 0; i < 9; i++) {
            points[i] = new double[]{0, 0};
        }
        points[9] = new double[]{100, 0};
        double[] median = GeometricMedian.median(points, 500);
        // 质心 = (10,0)
        double medianCost = GeometricMedian.totalDistance(points, median);
        double centroidCost = GeometricMedian.totalDistance(points, new double[]{10, 0});
        assertThat(medianCost).isLessThan(centroidCost);
        // 几何中位数应贴近原点簇（对离群稳健）
        assertThat(median[0]).isLessThan(1.0);
        // 迭代改进性：迭代 500 步目标 ≤ 迭代 1 步目标
        double[] one = GeometricMedian.median(points, 1);
        assertThat(medianCost).isLessThanOrEqualTo(GeometricMedian.totalDistance(points, one));
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        Random random = new Random(83);
        double[][] points = new double[20][];
        for (int i = 0; i < 20; i++) {
            points[i] = new double[]{random.nextGaussian() * 10, random.nextGaussian() * 10};
        }
        assertThat(GeometricMedian.median(points, 100))
                .containsExactly(GeometricMedian.median(points, 100));
        assertThatThrownBy(() -> GeometricMedian.median(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeometricMedian.median(new double[0][], 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeometricMedian.median(new double[][]{{1, 2}, {3}}, 10))
                .hasMessageContaining("维度");
        assertThatThrownBy(() -> GeometricMedian.median(points, 0))
                .hasMessageContaining("迭代次数");
    }
}
