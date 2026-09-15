package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1832 / T2866：覆写环形——FIFO、满覆最老、覆写计数、防御拷贝。 */
class OverwritingRingBufferTest {

    /** 容量内 FIFO 保序。 */
    @Test
    void fifoOrderWithinCapacity() {
        OverwritingRingBuffer<String> buffer = new OverwritingRingBuffer<>(4);
        IntStream.rangeClosed(1, 3).mapToObj(i -> "item-" + i).forEach(buffer::add);
        assertThat(buffer.items()).containsExactly("item-1", "item-2", "item-3");
        assertThat(buffer.stats().size()).isEqualTo(3);
        assertThat(buffer.stats().hasOverwritten()).isFalse();
    }

    /** 满则覆写最老：窗滑动 + 覆写计数诚实。 */
    @Test
    void overwriteOldestWhenFull() {
        OverwritingRingBuffer<Integer> buffer = new OverwritingRingBuffer<>(3);
        IntStream.rangeClosed(1, 5).forEach(buffer::add);
        assertThat(buffer.items()).containsExactly(3, 4, 5);
        assertThat(buffer.stats().size()).isEqualTo(3);
        assertThat(buffer.stats().overwrites()).isEqualTo(2);
        assertThat(buffer.stats().hasOverwritten()).isTrue();
    }

    /** 容量 1 退化：只留最新，每次都是覆写（首圈后）。 */
    @Test
    void capacityOneKeepsOnlyLatest() {
        OverwritingRingBuffer<String> buffer = new OverwritingRingBuffer<>(1);
        buffer.add("a");
        buffer.add("b");
        assertThat(buffer.items()).containsExactly("b");
        assertThat(buffer.stats().overwrites()).isEqualTo(1);
    }

    /** 防御拷贝：取快照后再追加不影响已取列表。 */
    @Test
    void itemsSnapshotIsDefensive() {
        OverwritingRingBuffer<String> buffer = new OverwritingRingBuffer<>(2);
        buffer.add("a");
        List<String> snapshot = buffer.items();
        buffer.add("b");
        assertThat(snapshot).containsExactly("a");
        assertThat(buffer.items()).containsExactly("a", "b");
    }

    /** 畸形入参 fail-fast：容量 < 1、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new OverwritingRingBuffer<String>(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity 不能小于 1");
        assertThatThrownBy(() -> new OverwritingRingBuffer<String>(2).add(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("元素不能为 null");
    }
}
