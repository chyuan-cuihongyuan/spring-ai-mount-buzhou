package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KMeansClusteringTest {

    private static double[][] threeClusters() {
        Random random = new Random(7);
        double[][] points = new double[30][2];
        for (int i = 0; i < 10; i++) {
            points[i] = new double[]{random.nextDouble() * 2, random.nextDouble() * 2};
            points[10 + i] = new double[]{20 + random.nextDouble() * 2, random.nextDouble() * 2};
            points[20 + i] = new double[]{random.nextDouble() * 2, 20 + random.nextDouble() * 2};
        }
        return points;
    }

    @Test
    void shouldSeparateThreeWellSpacedClusters() {
        double[][] points = threeClusters();
        int[] labels = KMeansClustering.assign(points, 3, 42, 100);
        for (int i = 1; i < 10; i++) {
            assertThat(labels[i]).isEqualTo(labels[0]);
        }
        for (int i = 11; i < 20; i++) {
            assertThat(labels[i]).isEqualTo(labels[10]);
        }
        assertThat(labels[0]).isNotEqualTo(labels[10]);
        assertThat(labels[10]).isNotEqualTo(labels[20]);
    }

    @Test
    void shouldBeDeterministicAndStopOnConvergence() {
        double[][] points = threeClusters();
        int[] first = KMeansClustering.assign(points, 3, 99, 100);
        int[] second = KMeansClustering.assign(points, 3, 99, 100);
        assertThat(first).isEqualTo(second);
        int[] converged = KMeansClustering.assign(points, 3, 99, 1);
        for (int label : converged) {
            assertThat(label).isBetween(0, 2);
        }
    }

    @Test
    void shouldFailFastOnBadInputs() {
        assertThatThrownBy(() -> KMeansClustering.assign(null, 2, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KMeansClustering.assign(new double[0][], 1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KMeansClustering.assign(new double[][]{{1, 2}, {3, 4}}, 3, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KMeansClustering.assign(new double[][]{{1, 2}, {3}}, 1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KMeansClustering.assign(new double[][]{{1, 2}}, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
