package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7044：FloydCycleDetector 合同——龟兔赛跑 O(1) 空间
 * 判圈。环长/入口手锚；无环 -1；随机函数图 vs HashSet
 * 圣像；fail-fast。
 */
class FloydCycleDetectorTest {

    @Test
    void handAnchoredCycle() {
        // 0→1→2→3→4→2（环 2→3→4→2，长 3，入口 2）
        FloydCycleDetector graph = new FloydCycleDetector(new int[]{1, 2, 3, 4, 2});
        assertThat(graph.hasCycle(0)).isTrue();
        assertThat(graph.cycleLength(0)).isEqualTo(3);
        assertThat(graph.cycleEntry(0)).isEqualTo(2);
    }

    @Test
    void acyclicReturnsMinusOne() {
        // 0→1→2（2 无出边——next[2] 越域语义在构造即拒）
        // 改用自环尾：0→1→2→2（环长 1，入口 2）
        FloydCycleDetector selfLoop = new FloydCycleDetector(new int[]{1, 2, 2});
        assertThat(selfLoop.cycleLength(0)).isEqualTo(1);
        assertThat(selfLoop.cycleEntry(0)).isEqualTo(2);
        assertThatThrownBy(() -> new FloydCycleDetector(new int[]{1, 5}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void randomFunctionalGraphsMatchHashSetOracle() {
        Random rng = new Random(7044L);
        for (int round = 0; round < 200; round++) {
            int n = 2 + rng.nextInt(40);
            int[] next = new int[n];
            for (int i = 0; i < n; i++) {
                next[i] = rng.nextInt(n);
            }
            FloydCycleDetector detector = new FloydCycleDetector(next);
            int start = rng.nextInt(n);
            int fast = detector.cycleLength(start);
            int entry = detector.cycleEntry(start);
            // HashSet 圣像
            java.util.Map<Integer, Integer> firstSeen = new java.util.HashMap<>();
            int cursor = start;
            int step = 0;
            int oracleEntry = -1;
            int oracleLength = -1;
            while (cursor < n && !firstSeen.containsKey(cursor)) {
                firstSeen.put(cursor, step);
                cursor = next[cursor];
                step++;
            }
            if (cursor < n) {
                oracleEntry = cursor;
                oracleLength = step - firstSeen.get(cursor);
            }
            assertThat(fast).as("round %d", round).isEqualTo(oracleLength);
            if (oracleLength != -1) {
                assertThat(entry).isEqualTo(oracleEntry);
            } else {
                assertThat(entry).isEqualTo(-1);
            }
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new FloydCycleDetector(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FloydCycleDetector(new int[]{-1})).isInstanceOf(IllegalArgumentException.class);
        FloydCycleDetector graph = new FloydCycleDetector(new int[]{0, 1});
        assertThatThrownBy(() -> graph.cycleLength(9)).isInstanceOf(IllegalArgumentException.class);
    }
}
