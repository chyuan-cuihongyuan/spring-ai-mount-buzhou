package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3024 / T5050：Grubbs 合同——明显离群检出（正值/负值双侧）、
 * 干净样本不误报、G 统计量手算（{1,2,3,4,100}→≈1.788 超 n=5 临界
 * 1.672）、零方差诚实拒绝、表界 fail-fast、矩读数回带。
 */
class GrubbsOutlierTest {

    @Test
    void obviousHighOutlierShouldBeDetected() {
        GrubbsOutlier.Result result = GrubbsOutlier.test(10, 10.2, 9.8, 10.1, 9.9, 30);
        assertThat(result.outlierDetected()).isTrue();
        assertThat(result.outlier()).isEqualTo(30.0);
        assertThat(result.statistic()).isGreaterThan(result.criticalValue());
    }

    @Test
    void obviousLowOutlierShouldBeDetected() {
        GrubbsOutlier.Result result = GrubbsOutlier.test(5, 5.1, 4.9, 5.05, -100);
        assertThat(result.outlierDetected()).isTrue();
        assertThat(result.outlier()).isEqualTo(-100.0);
    }

    @Test
    void cleanSampleShouldNotFalselyFlag() {
        GrubbsOutlier.Result result = GrubbsOutlier.test(9.8, 10.0, 10.1, 9.9, 10.2, 10.05);
        assertThat(result.outlierDetected()).isFalse();
        assertThat(result.outlier()).isNull();
    }

    @Test
    void statisticShouldMatchHandComputation() {
        // {1,2,3,4,100}: mean=22、s=√1902.5≈43.618、G=78/43.618≈1.7879
        GrubbsOutlier.Result result = GrubbsOutlier.test(1, 2, 3, 4, 100);
        assertThat(result.mean()).isEqualTo(22.0);
        assertThat(result.stdDev()).isCloseTo(Math.sqrt(1902.5), within(1e-9));
        assertThat(result.statistic()).isCloseTo(78 / Math.sqrt(1902.5), within(1e-9));
        assertThat(result.criticalValue()).isEqualTo(1.672);  // n=5 表位
        assertThat(result.outlierDetected()).isTrue();
        assertThat(result.outlier()).isEqualTo(100.0);
    }

    @Test
    void constantSamplesShouldBeRejectedHonestly() {
        assertThatThrownBy(() -> GrubbsOutlier.test(7, 7, 7, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tableBoundsShouldBeEnforced() {
        assertThatThrownBy(() -> GrubbsOutlier.test(1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GrubbsOutlier.test(1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
                28, 29, 30, 31, 32, 33))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GrubbsOutlier.test((double[]) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criticalValueShouldGrowWithSampleCount() {
        double critical5 = GrubbsOutlier.test(1, 2, 3, 4, 5).criticalValue();
        double critical20 = GrubbsOutlier.test(1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20).criticalValue();
        assertThat(critical20).isGreaterThan(critical5);
    }
}
