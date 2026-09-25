package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6030：BuddyAllocator 合同——2 的幂分裂/伙伴合并。
 * 全量分配+全量释放回归单块；分裂次序确定性；双重释放
 * fail-fast。
 */
class BuddyAllocatorTest {

    @Test
    void fullAllocateThenFreeMergesBackToSingleBlock() {
        BuddyAllocator pool = new BuddyAllocator(4);
        long[] blocks = new long[16];
        for (int i = 0; i < 16; i++) {
            blocks[i] = pool.allocate(0);
        }
        assertThat(pool.freeBlocks(0)).isZero();
        assertThatThrownBy(() -> pool.allocate(0)).as("池满无块").isInstanceOf(IllegalArgumentException.class);
        for (long b : blocks) {
            pool.free(b, 0);
        }
        assertThat(pool.freeBlocks(4)).as("伙伴逐级合并回整块").isEqualTo(1);
    }

    @Test
    void splitTakesLowHalfFirst() {
        BuddyAllocator pool = new BuddyAllocator(3);
        assertThat(pool.allocate(0)).isZero();
        assertThat(pool.allocate(0)).isEqualTo(1L);
        assertThat(pool.allocate(0)).isEqualTo(2L);
        assertThat(pool.allocate(1)).isEqualTo(4L);
    }

    @Test
    void interleavedAllocFreeIsConsistent() {
        BuddyAllocator pool = new BuddyAllocator(3);
        long a = pool.allocate(1);
        long b = pool.allocate(0);
        long c = pool.allocate(1);
        pool.free(a, 1);
        assertThat(pool.freeBlocks(1)).as("伙伴未整块不可并——order1 有 @0 与 @6").isEqualTo(2);
        pool.free(b, 0);
        pool.free(c, 1);
        assertThat(pool.freeBlocks(3)).as("全部释放后合并回整块").isEqualTo(1);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new BuddyAllocator(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BuddyAllocator(21)).isInstanceOf(IllegalArgumentException.class);
        BuddyAllocator pool = new BuddyAllocator(3);
        assertThatThrownBy(() -> pool.allocate(4)).isInstanceOf(IllegalArgumentException.class);
        long block = pool.allocate(0);
        pool.free(block, 0);
        assertThatThrownBy(() -> pool.free(block, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pool.free(0, 5)).isInstanceOf(IllegalArgumentException.class);
    }
}
