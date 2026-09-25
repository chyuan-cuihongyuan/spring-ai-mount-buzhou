package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6020：QuadTree 合同——象限递归分割区域查询。
 * 暴力过滤圣像（含字典序）；边界象限一致性；fail-fast。
 */
class QuadTreeTest {

    @Test
    void rangeQueriesMatchBruteForceOracle() {
        Random rng = new Random(6020L);
        QuadTree tree = QuadTree.overBounds(0, 0, 1023, 1023);
        List<QuadTree.Point> points = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            long x = rng.nextInt(1024);
            long y = rng.nextInt(1024);
            tree.insert(x, y);
            points.add(new QuadTree.Point(x, y));
        }
        assertThat(tree.size()).isEqualTo(300);
        for (int q = 0; q < 30; q++) {
            long x1 = rng.nextInt(1024);
            long y1 = rng.nextInt(1024);
            long x2 = Math.min(1023, x1 + rng.nextInt(400));
            long y2 = Math.min(1023, y1 + rng.nextInt(400));
            QuadTree.Rect rect = new QuadTree.Rect(x1, y1, x2, y2);
            List<QuadTree.Point> expected = points.stream()
                    .filter(p -> p.x() >= x1 && p.x() <= x2 && p.y() >= y1 && p.y() <= y2)
                    .sorted((a, b) -> {
                        int byX = Long.compare(a.x(), b.x());
                        return byX != 0 ? byX : Long.compare(a.y(), b.y());
                    })
                    .toList();
            assertThat(tree.query(rect)).as("rect %s", rect).containsExactlyElementsOf(expected);
        }
    }

    @Test
    void boundaryLinePointsConsistent() {
        QuadTree tree = QuadTree.overBounds(0, 0, 15, 15);
        tree.insert(7, 7);
        tree.insert(8, 8);
        tree.insert(7, 8);
        tree.insert(8, 7);
        tree.insert(0, 0);
        tree.insert(15, 15);
        List<QuadTree.Point> center = tree.query(new QuadTree.Rect(7, 7, 8, 8));
        assertThat(center).hasSize(4);
        assertThat(tree.query(new QuadTree.Rect(0, 0, 15, 15))).hasSize(6);
        assertThat(tree.query(new QuadTree.Rect(15, 15, 15, 15)))
                .containsExactly(new QuadTree.Point(15, 15));
    }

    @Test
    void degenerateSingleCellRootAccumulates() {
        QuadTree tree = QuadTree.overBounds(0, 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            tree.insert(0, 0);
        }
        assertThat(tree.size()).isEqualTo(10);
        assertThat(tree.query(new QuadTree.Rect(0, 0, 0, 0))).hasSize(10);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> QuadTree.overBounds(10, 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        QuadTree tree = QuadTree.overBounds(0, 0, 10, 10);
        assertThatThrownBy(() -> tree.insert(11, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.insert(5, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tree.query(new QuadTree.Rect(5, 5, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tree.query(new QuadTree.Rect(0, 0, 10, 10))).isEmpty();
    }
}
