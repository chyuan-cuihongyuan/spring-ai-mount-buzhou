package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wilson 置信区间测试（spec 1630 / T2411–T2412 / impl 1183）：
 * 边界合法（[0,1] 内、含点估计）、小样本宽度大、极端比例不出负值越界、
 * 退化输入零区间。
 */
class WilsonIntervalTest {

    @Test
    void intervalContainsPointEstimateAndStaysInUnitRange() {
        double[] ci = WilsonInterval.of(7, 10); // 0.7 胜率
        assertThat(ci[0]).isGreaterThan(0.0).isLessThan(0.7);
        assertThat(ci[1]).isGreaterThan(0.7).isLessThanOrEqualTo(1.0);
    }

    @Test
    void extremeProportionsNeverGoOutOfRange() {
        double[] allWin = WilsonInterval.of(10, 10);
        assertThat(allWin[0]).isGreaterThanOrEqualTo(0.0);
        assertThat(allWin[1]).isEqualTo(1.0);
        double[] allLose = WilsonInterval.of(0, 10);
        assertThat(allLose[0]).isEqualTo(0.0);
        assertThat(allLose[1]).isLessThanOrEqualTo(1.0);
    }

    @Test
    void smallSampleWiderThanLarge() {
        double[] small = WilsonInterval.of(7, 10);
        double[] large = WilsonInterval.of(700, 1000);
        assertThat(small[1] - small[0]).isGreaterThan(large[1] - large[0]); // 不确定性随样本收缩
    }

    @Test
    void degenerateInputsReturnZeroInterval() {
        assertThat(WilsonInterval.of(0, 0)).containsExactly(0.0, 0.0);
        assertThat(WilsonInterval.of(-1, 10)).containsExactly(0.0, 0.0);
        assertThat(WilsonInterval.of(11, 10)).containsExactly(0.0, 0.0);
        assertThat(WilsonInterval.of(5, 10, 0)).containsExactly(0.0, 0.0);
    }
}
