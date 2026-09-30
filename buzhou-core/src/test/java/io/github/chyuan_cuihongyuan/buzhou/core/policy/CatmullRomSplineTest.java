package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * CatmullRomSpline 契约测试（spec 10015 / X10032）：过点圣像 +
 * 共线退化线性 + 两点直线 + 参数域 + fail-fast + 确定性。
 */
class CatmullRomSplineTest {

    private static final double EPSILON = 1e-9;

    @Test
    void shouldPassThroughAllControlPoints() {
        double[][] cps = {{0, 0}, {1, 2}, {3, -1}, {5, 1}, {6, 3}};
        CatmullRomSpline spline = new CatmullRomSpline(cps);
        for (int i = 0; i < cps.length; i++) {
            double[] p = spline.point(i);
            assertThat(p[0]).as("过点 x[%d]", i).isCloseTo(cps[i][0], within(EPSILON));
            assertThat(p[1]).as("过点 y[%d]", i).isCloseTo(cps[i][1], within(EPSILON));
        }
    }

    @Test
    void shouldReduceToLinearOnCollinearPoints() {
        double[][] line = {{0, 0}, {1, 0}, {2, 0}, {3, 0}};
        CatmullRomSpline spline = new CatmullRomSpline(line);
        for (double t = 1.0; t <= 2.0; t += 0.25) {
            double[] p = spline.point(t);
            assertThat(p[0]).as("中段 t=%f 线性", t).isCloseTo(t, within(1e-7));
            assertThat(p[1]).isCloseTo(0.0, within(EPSILON));
        }
        for (double t = 0.0; t <= 3.0; t += 0.1) {
            assertThat(spline.point(t)[1]).isCloseTo(0.0, within(EPSILON));
        }
    }

    @Test
    void shouldInterpolateStraightWithTwoPoints() {
        double[][] ends = {{0, 0}, {10, 20}};
        CatmullRomSpline spline = new CatmullRomSpline(ends);
        double[] mid = spline.point(0.5);
        assertThat(mid[0]).isCloseTo(5.0, within(EPSILON));
        assertThat(mid[1]).isCloseTo(10.0, within(EPSILON));
    }

    @Test
    void shouldStayDeterministic() {
        double[][] cps = {{0, 0}, {1, 2}, {3, -1}};
        CatmullRomSpline spline = new CatmullRomSpline(cps);
        double[] first = spline.point(0.37);
        double[] second = spline.point(0.37);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> new CatmullRomSpline(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CatmullRomSpline(new double[][]{{1, 2}}))
                .isInstanceOf(IllegalArgumentException.class);
        CatmullRomSpline spline = new CatmullRomSpline(new double[][]{{0, 0}, {1, 1}});
        assertThatThrownBy(() -> spline.point(-0.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> spline.point(1.1)).isInstanceOf(IllegalArgumentException.class);
    }
}
