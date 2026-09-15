package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1740 / T2682：PiiChannelMatrix 直测——矩阵归账/降序/匿名桶。
 */
class PiiChannelMatrixTest {

    @Test
    void matrixTalliesAndSortsDescending() {
        var matrix = new PiiChannelMatrix();
        matrix.record(PiiChannelMatrix.Channel.INPUT, "phone");
        matrix.record(PiiChannelMatrix.Channel.INPUT, "phone");
        matrix.record(PiiChannelMatrix.Channel.STREAM, "email");
        var census = matrix.census();
        assertThat(matrix.total()).isEqualTo(3);
        assertThat(census.entrySet().iterator().next().getKey()).isEqualTo("INPUT:phone");
        assertThat(census.entrySet().iterator().next().getValue()).isEqualTo(2L);
        assertThat(census).containsKey("STREAM:email");
    }

    @Test
    void blankTypeGoesUnknown() {
        var matrix = new PiiChannelMatrix();
        matrix.record(PiiChannelMatrix.Channel.EXPORT, null);
        assertThat(matrix.census()).containsKey("EXPORT:_unknown_");
    }

    @Test
    void emptyMatrix() {
        var matrix = new PiiChannelMatrix();
        assertThat(matrix.census()).isEmpty();
        assertThat(matrix.total()).isZero();
    }
}
