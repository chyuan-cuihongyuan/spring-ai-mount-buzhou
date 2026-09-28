package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7028：SortedRunMerge 合同——k 路归并+墓碑清理。
 * 手锚（新覆盖旧/墓碑掩埋）；随机游程 vs TreeMap 圣像；
 * 乱序 fail-fast。
 */
class SortedRunMergeTest {

    @Test
    void newestWinsAndTombstonePurges() {
        List<List<long[]>> runs = List.of(
                SortedRunMerge.run(new long[]{2, 20}, new long[]{4, SortedRunMerge.TOMBSTONE}),
                SortedRunMerge.run(new long[]{1, 10}, new long[]{2, 99}, new long[]{4, 40}),
                SortedRunMerge.run(new long[]{2, 7}, new long[]{3, 30}));
        List<long[]> merged = SortedRunMerge.merge(runs);
        assertThat(merged).hasSize(3);
        assertThat(merged.get(0)).containsExactly(1L, 10L);
        assertThat(merged.get(1)).containsExactly(2L, 20L);
        assertThat(merged.get(2)).containsExactly(3L, 30L);
    }

    @Test
    void singleRunPassThrough() {
        List<List<long[]>> runs = List.of(
                SortedRunMerge.run(new long[]{1, 1}, new long[]{2, 2}));
        assertThat(SortedRunMerge.merge(runs)).hasSize(2);
    }

    @Test
    void randomRunsMatchNewestWinsOracle() {
        Random rng = new Random(7028L);
        for (int round = 0; round < 200; round++) {
            int runCount = 1 + rng.nextInt(4);
            List<List<long[]>> runs = new java.util.ArrayList<>();
            TreeMap<Long, Long> oracle = new TreeMap<>();
            for (int r = runCount - 1; r >= 0; r--) {
                List<long[]> run = new java.util.ArrayList<>();
                int size = 1 + rng.nextInt(10);
                long key = rng.nextInt(10);
                for (int i = 0; i < size; i++) {
                    key += 1 + rng.nextInt(4);
                    long value = rng.nextInt(100);
                    if (rng.nextBoolean() && value > 50) {
                        value = SortedRunMerge.TOMBSTONE;
                    }
                    run.add(new long[]{key, value});
                }
                runs.add(0, run);
            }
            for (int r = runs.size() - 1; r >= 0; r--) {
                for (long[] entry : runs.get(r)) {
                    oracle.put(entry[0], entry[1]);
                }
            }
            List<long[]> merged = SortedRunMerge.merge(runs);
            List<long[]> expected = new java.util.ArrayList<>();
            for (Map.Entry<Long, Long> entry : oracle.entrySet()) {
                if (entry.getValue() != SortedRunMerge.TOMBSTONE) {
                    expected.add(new long[]{entry.getKey(), entry.getValue()});
                }
            }
            assertThat(merged).as("round %d", round)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> SortedRunMerge.merge(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SortedRunMerge.merge(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SortedRunMerge.merge(
                List.of(SortedRunMerge.run(new long[]{2, 1}, new long[]{1, 1}))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(SortedRunMerge.merge(
                List.of(SortedRunMerge.run(new long[]{1, 1}, new long[]{1, 2}))))
                .hasSize(1);
    }
}
