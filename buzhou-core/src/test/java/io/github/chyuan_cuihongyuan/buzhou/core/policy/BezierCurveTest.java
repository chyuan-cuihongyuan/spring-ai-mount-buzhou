package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BezierCurveTest {

    @Test
    void shouldMatchBernsteinAnchors() {
        // 线性（2 控制点）：中点恰 (0.5P0+0.5P1)
        double[] mid = BezierCurve.pointAt(new double[][]{{0, 0}, {10, 20}}, 0.5);
        assertThat(mid[0]).isCloseTo(5.0, within(1e-12));
        assertThat(mid[1]).isCloseTo(10.0, within(1e-12));
        // 二次 t=0.5：0.25P0+0.5P1+0.25P2
        double[] quad = BezierCurve.pointAt(new double[][]{{0, 0}, {2, 8}, {4, 0}}, 0.5);
        assertThat(quad[0]).isCloseTo(2.0, within(1e-12));
        assertThat(quad[1]).isCloseTo(4.0, within(1e-12));
        // 三次 t=0.5：(P0+3P1+3P2+P3)/8——CSS ease 级公式
        double[] cubic = BezierCurve.pointAt(
                new double[][]{{0, 0}, {1, 3}, {3, 3}, {4, 0}}, 0.5);
        assertThat(cubic[0]).isCloseTo(2.0, within(1e-12));
        assertThat(cubic[1]).isCloseTo(2.25, within(1e-12)); // (0+3·3+3·3+0)/8
        // 端点插值不变量
        double[][] pts = {{1, 2}, {5, -3}, {-2, 8}, {7, 7}, {0, 0}};
        assertThat(BezierCurve.pointAt(pts, 0)).containsExactly(1.0, 2.0);
        assertThat(BezierCurve.pointAt(pts, 1)).containsExactly(0.0, 0.0);
        // 单点退化（恒点）
        assertThat(BezierCurve.pointAt(new double[][]{{3, 4}}, 0.77)).containsExactly(3.0, 4.0);
    }

    @Test
    void shouldStayInsideConvexHullOfControls() {
        // 凸包包含不变量圣像：随机控制点——全部采样点 x∈[min,max]×[min,max]（包围盒必要条件）
        Random random = new Random(97);
        for (int t = 0; t < 30; t++) {
            double[][] pts = new double[4][];
            for (int i = 0; i < 4; i++) {
                pts[i] = new double[]{random.nextGaussian() * 10, random.nextGaussian() * 10};
            }
            double minX = pts[0][0];
            double maxX = pts[0][0];
            double minY = pts[0][1];
            double maxY = pts[0][1];
            for (double[] p : pts) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
            for (double s = 0; s <= 1.0001; s += 0.05) {
                double[] point = BezierCurve.pointAt(pts, Math.min(s, 1.0));
                assertThat(point[0]).isBetween(minX, maxX);
                assertThat(point[1]).isBetween(minY, maxY);
            }
        }
    }

    @Test
    void shouldFlattenWithEndpointsAndFailFast() {
        List<double[]> polyline = BezierCurve.flatten(new double[][]{{0, 0}, {1, 1}, {2, 0}}, 4);
        assertThat(polyline).hasSize(5);
        assertThat(polyline.get(0)).containsExactly(0.0, 0.0);
        assertThat(polyline.get(4)).containsExactly(2.0, 0.0);
        // 与逐点求值一致
        assertThat(polyline.get(2)).containsExactly(BezierCurve.pointAt(
                new double[][]{{0, 0}, {1, 1}, {2, 0}}, 0.5));
        assertThatThrownBy(() -> BezierCurve.pointAt(null, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BezierCurve.pointAt(new double[][]{{0, 0}}, -0.1))
                .hasMessageContaining("t∈[0,1]");
        assertThatThrownBy(() -> BezierCurve.pointAt(new double[][]{{0, 0}, {1}}, 0.5))
                .hasMessageContaining("维度");
        assertThatThrownBy(() -> BezierCurve.flatten(new double[][]{{0, 0}, {1, 1}}, 0))
                .hasMessageContaining("段数");
    }
}
