package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4021 / T6044：工作窃取合同——对半定量、冷端搬运、
 * 连续摊平、下限与畸形 fail-fast。
 */
class WorkStealingSplitTest {

    @Test
    void stealCountShouldHalveWithBounds() {
        WorkStealingSplit splitter = new WorkStealingSplit(1);
        assertThat(splitter.stealCount(0)).isZero();   // 空——无可偷
        assertThat(splitter.stealCount(1)).isEqualTo(1);   // 下限保底
        assertThat(splitter.stealCount(2)).isEqualTo(1);
        assertThat(splitter.stealCount(3)).isEqualTo(1);   // 3/2=1
        assertThat(splitter.stealCount(4)).isEqualTo(2);
        assertThat(splitter.stealCount(10)).isEqualTo(5);
        assertThat(splitter.stealCount(11)).isEqualTo(5);
    }

    @Test
    void stealShouldTakeFromColdEndLeavingHotEnd() {
        WorkStealingSplit splitter = WorkStealingSplit.defaults();
        Deque<String> victim = new ArrayDeque<>();
        for (String item : new String[] {"a", "b", "c", "d", "e"}) {
            victim.addFirst(item);   // owner 热端压入——first=e（最新）
        }
        List<String> stolen = splitter.stealFrom(victim);   // 5→偷 2
        assertThat(stolen).containsExactly("a", "b");   // 冷端（最老）先离队
        assertThat(victim.size()).isEqualTo(3);
        assertThat(victim.pollFirst()).isEqualTo("e");   // owner 热端不动
        assertThat(victim.pollLast()).isEqualTo("c");
    }

    @Test
    void repeatedStealsShouldFlattenGradient() {
        WorkStealingSplit splitter = WorkStealingSplit.defaults();
        Deque<Integer> victim = new ArrayDeque<>();
        for (int i = 0; i < 100; i++) {
            victim.addFirst(i);
        }
        assertThat(splitter.stealFrom(victim)).hasSize(50);   // 100→50
        assertThat(splitter.stealFrom(victim)).hasSize(25);   // 50→25
        assertThat(splitter.stealFrom(victim)).hasSize(12);   // 25→12
        assertThat(victim).hasSize(13);
    }

    @Test
    void singletonAndEmptyVictimsShouldBehave() {
        WorkStealingSplit splitter = WorkStealingSplit.defaults();
        Deque<Integer> singleton = new ArrayDeque<>(List.of(7));
        assertThat(splitter.stealFrom(singleton)).containsExactly(7);
        assertThat(singleton).isEmpty();
        assertThat(splitter.stealFrom(new ArrayDeque<Integer>())).isEmpty();
        assertThat(splitter.minSteal()).isEqualTo(1);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new WorkStealingSplit(0))
                .isInstanceOf(IllegalArgumentException.class);
        WorkStealingSplit splitter = WorkStealingSplit.defaults();
        assertThatThrownBy(() -> splitter.stealCount(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> splitter.stealFrom(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
