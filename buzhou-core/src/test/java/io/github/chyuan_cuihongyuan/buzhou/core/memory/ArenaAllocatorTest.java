package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6034：ArenaAllocator 合同——线性 bump+整池回收。
 * 偏移确定性；水位/峰值读数；freeAll 保留峰值；越界 fail-fast。
 */
class ArenaAllocatorTest {

    @Test
    void sequentialAllocationIsDeterministic() {
        ArenaAllocator arena = new ArenaAllocator(100);
        assertThat(arena.allocate(10)).isZero();
        assertThat(arena.allocate(20)).isEqualTo(10);
        assertThat(arena.allocate(5)).isEqualTo(30);
        assertThat(arena.watermark()).isEqualTo(35);
        assertThat(arena.highWaterMark()).isEqualTo(35);
    }

    @Test
    void freeAllResetsWatermarkKeepsPeak() {
        ArenaAllocator arena = new ArenaAllocator(100);
        arena.allocate(30);
        arena.allocate(20);
        assertThat(arena.highWaterMark()).isEqualTo(50);
        arena.freeAll();
        assertThat(arena.watermark()).isZero();
        assertThat(arena.highWaterMark()).as("峰值不被回收清零").isEqualTo(50);
        arena.allocate(40);
        assertThat(arena.highWaterMark()).as("峰值取历史最大").isEqualTo(50);
    }

    @Test
    void readWriteWithinAllocatedRegion() {
        ArenaAllocator arena = new ArenaAllocator(64);
        long base = arena.allocate(8);
        arena.write(base + 3, 42);
        assertThat(arena.read(base + 3)).isEqualTo(42);
        arena.freeAll();
        arena.allocate(8);
        assertThat(arena.read(3)).as("回收后清零").isZero();
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new ArenaAllocator(0)).isInstanceOf(IllegalArgumentException.class);
        ArenaAllocator arena = new ArenaAllocator(10);
        assertThatThrownBy(() -> arena.allocate(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> arena.allocate(11)).isInstanceOf(IllegalArgumentException.class);
        long base = arena.allocate(5);
        assertThatThrownBy(() -> arena.read(5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> arena.write(10, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(base).isZero();
    }
}
