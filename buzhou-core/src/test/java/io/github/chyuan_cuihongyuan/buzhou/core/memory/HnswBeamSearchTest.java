package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4016 / T6034：HNSW 合同——网格精确召回、增量保持、
 * k 上限与层级读数、畸形 fail-fast。
 */
class HnswBeamSearchTest {

    /** 10×10 网格（间距 10），id = 行×10+列——无距离并列的近邻结构。 */
    private static List<double[]> gridPoints() {
        List<double[]> points = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                points.add(new double[] {row * 10.0 + col * 0.1, col * 10.0 + row * 0.1});
            }
        }
        return points;
    }

    /** 暴力 kNN（对账基准）。 */
    private static TreeSet<Integer> bruteTop(double[] query, List<double[]> points, int k) {
        record Cand(double dist, int id) {
        }
        List<Cand> all = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            double dx = query[0] - points.get(i)[0];
            double dy = query[1] - points.get(i)[1];
            all.add(new Cand(Math.sqrt(dx * dx + dy * dy), i));
        }
        all.sort((a, b) -> a.dist() != b.dist() ? Double.compare(a.dist(), b.dist())
                : Integer.compare(a.id(), b.id()));
        TreeSet<Integer> top = new TreeSet<>();
        for (int i = 0; i < k; i++) {
            top.add(all.get(i).id());
        }
        return top;
    }

    @Test
    void gridQueriesShouldRecallExactNeighbors() {
        List<double[]> points = gridPoints();
        HnswBeamSearch index = new HnswBeamSearch(8, 32, new Random(42));
        for (int i = 0; i < points.size(); i++) {
            index.add(i, points.get(i));
        }
        assertThat(index.size()).isEqualTo(100);
        int[][] probes = {{12, 34}, {55, 12}, {97, 88}, {3, 71}};
        for (int[] probe : probes) {
            double[] query = {probe[0], probe[1]};
            assertThat(index.search(query, 1))
                    .as("top1 精确（探针 %s）", (Object) probe)
                    .first()
                    .isEqualTo(bruteTop(query, points, 1).first());
            assertThat(new TreeSet<>(index.search(query, 5)))
                    .as("top5 全召回（探针 %s）", (Object) probe)
                    .isEqualTo(bruteTop(query, points, 5));
        }
    }

    @Test
    void incrementalAddsShouldKeepRecall() {
        List<double[]> points = gridPoints();
        HnswBeamSearch index = new HnswBeamSearch(6, 24, new Random(7));
        for (int i = 0; i < 60; i++) {
            index.add(i, points.get(i));
        }
        for (int i = 60; i < 100; i++) {
            index.add(i, points.get(i));
        }
        double[] query = {44, 42};
        assertThat(new TreeSet<>(index.search(query, 3))).isEqualTo(
                bruteTop(query, points, 3));   // 集合口径（距离序 vs id 序解耦）
        assertThat(index.search(query, 1)).first().isEqualTo(bruteTop(query, points, 1).first());
        assertThat(index.maxLevel()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void kBeyondSizeShouldReturnAll() {
        List<double[]> points = gridPoints();
        HnswBeamSearch index = new HnswBeamSearch(8, 32, new Random(3));
        for (int i = 0; i < 25; i++) {
            index.add(i, points.get(i));
        }
        assertThat(index.search(points.get(0), 100)).hasSize(25);
        assertThat(index.search(points.get(0), 3)).hasSize(3);
    }

    @Test
    void emptyIndexShouldReturnEmptyAndFailFastOnBadInput() {
        HnswBeamSearch index = new HnswBeamSearch(4, 16, new Random(1));
        assertThat(index.search(new double[] {1, 2}, 3)).isEmpty();
        assertThat(index.size()).isZero();
        index.add(1, new double[] {1, 2});
        assertThatThrownBy(() -> index.search(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.search(new double[] {1, 2}, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.search(new double[] {1, 2, 3}, 1))   // 维度不一致
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.add(1, new double[] {5, 6}))   // id 重复
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.add(2, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HnswBeamSearch(1, 8, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
