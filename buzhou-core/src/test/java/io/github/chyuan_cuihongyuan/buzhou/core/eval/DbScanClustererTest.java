package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbScanClustererTest {

    @Test
    void shouldSplitTwoBlobsAndFlagOutlier() {
        double[][] points = new double[13][2];
        Random random = new Random(5);
        for (int i = 0; i < 5; i++) {
            points[i] = new double[]{random.nextDouble(), random.nextDouble()};
            points[5 + i] = new double[]{50 + random.nextDouble(), 50 + random.nextDouble()};
        }
        points[10] = new double[]{500, 500};
        points[11] = new double[]{25, 25};
        points[12] = new double[]{2, 2};
        int[] labels = DbScanClusterer.cluster(points, 2.0, 2);
        assertThat(labels[0]).isNotEqualTo(DbScanClusterer.NOISE);
        assertThat(labels[5]).isNotEqualTo(DbScanClusterer.NOISE);
        assertThat(labels[0]).isNotEqualTo(labels[5]);
        for (int i = 1; i < 5; i++) {
            assertThat(labels[i]).isEqualTo(labels[0]);
            assertThat(labels[5 + i]).isEqualTo(labels[5]);
        }
        assertThat(labels[10]).isEqualTo(DbScanClusterer.NOISE);
        assertThat(labels[11]).isEqualTo(DbScanClusterer.NOISE);
    }

    @Test
    void shouldBeDeterministic() {
        Random random = new Random(8031);
        double[][] points = new double[40][2];
        for (int i = 0; i < 40; i++) {
            points[i] = new double[]{random.nextDouble() * 10, random.nextDouble() * 10};
        }
        int[] first = DbScanClusterer.cluster(points, 2.0, 3);
        int[] second = DbScanClusterer.cluster(points, 2.0, 3);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldFailFastOnBadInputs() {
        assertThatThrownBy(() -> DbScanClusterer.cluster(null, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DbScanClusterer.cluster(new double[0][], 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DbScanClusterer.cluster(new double[][]{{1, 2}}, 0, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DbScanClusterer.cluster(new double[][]{{1, 2}}, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
