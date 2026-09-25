package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6014：GorillaXor 合同——浮点序列 XOR 三态位流压缩。
 * 光滑序列无损+高压缩率；随机序列无损；特殊值按位保留；
 * 确定性位流；fail-fast。
 */
class GorillaXorTest {

    private static final double NAN_ODD = Double.longBitsToDouble(0x7ff8000000000001L);

    private static void assertRoundTrip(double[] series) {
        GorillaXor compressed = GorillaXor.compress(series);
        double[] decoded = compressed.decompress();
        long[] expectedBits = new long[series.length];
        for (int i = 0; i < series.length; i++) {
            expectedBits[i] = Double.doubleToRawLongBits(series[i]);
        }
        assertThat(series).hasSameSizeAs(decoded);
        long[] actualBits = new long[decoded.length];
        for (int i = 0; i < decoded.length; i++) {
            actualBits[i] = Double.doubleToRawLongBits(decoded[i]);
        }
        assertThat(actualBits).as("按位无损").containsExactly(expectedBits);
        assertThat(compressed.count()).isEqualTo(series.length);
    }

    @Test
    void smoothSeriesRoundTripsAndCompresses() {
        double[] series = new double[500];
        for (int i = 0; i < series.length; i++) {
            series[i] = 100.0 + (i / 4) * 0.25;
        }
        GorillaXor compressed = GorillaXor.compress(series);
        assertRoundTrip(series);
        assertThat(compressed.compressedBits())
                .as("量化步进序列压缩率显著优于 64×n")
                .isLessThan((int) (compressed.originalBits() / 3));
    }

    @Test
    void randomSeriesStillLossless() {
        double[] series = new double[200];
        Random rng = new Random(6010L);
        for (int i = 0; i < series.length; i++) {
            series[i] = Double.longBitsToDouble(rng.nextLong());
        }
        GorillaXor compressed = GorillaXor.compress(series);
        assertRoundTrip(series);
    }

    @Test
    void specialValuesPreservedBitwise() {
        double[] series = {0.0, -0.0, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, NAN_ODD, 1.5, 1.5, Double.MIN_VALUE,
                Double.MAX_VALUE, 0.0};
        assertRoundTrip(series);
    }

    @Test
    void identicalValuesCostOneBitEach() {
        double[] series = new double[100];
        Arrays.fill(series, 42.0);
        GorillaXor compressed = GorillaXor.compress(series);
        assertRoundTrip(series);
        assertThat(compressed.compressedBits())
                .as("首值 64 位+其余每值 1 位")
                .isEqualTo(64 + 99);
    }

    @Test
    void deterministicBitstream() {
        double[] series = {1.0, 1.5, 2.25, 2.25, 3.5};
        assertThat(GorillaXor.compress(series).wordsCopy())
                .containsExactly(GorillaXor.compress(series).wordsCopy());
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> GorillaXor.compress(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(GorillaXor.compress(new double[0]).count()).isZero();
    }
}
