package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2040 / T3184：可冻结分段缓冲合同——容量满自动封冻、手动封冻
 * 空段 no-op、drain 取走清空可变段保留、快照追加序、计数对账、畸形
 * fail-fast。
 */
class FreezableBufferTest {

    @Test
    void fullMutableSegmentShouldAutoFreeze() {
        FreezableBuffer<String> buffer = new FreezableBuffer<>(3);
        buffer.append("a");
        buffer.append("b");
        assertThat(buffer.frozenCount()).isZero(); // 未满不冻
        buffer.append("c"); // 满 3
        buffer.append("d"); // 触发封冻开新段
        assertThat(buffer.frozenCount()).isEqualTo(1);
        assertThat(buffer.mutableSize()).isEqualTo(1);
        assertThat(buffer.totalSize()).isEqualTo(4);
    }

    @Test
    void manualFreezeShouldSealMutableSegment() {
        FreezableBuffer<Integer> buffer = new FreezableBuffer<>(100);
        buffer.append(1);
        buffer.append(2);
        buffer.freeze(); // 手动封冻
        assertThat(buffer.frozenCount()).isEqualTo(1);
        assertThat(buffer.mutableSize()).isZero();
        buffer.freeze(); // 空段封冻 no-op
        assertThat(buffer.frozenCount()).isEqualTo(1);
    }

    @Test
    void drainShouldTakeFrozenAndKeepMutable() {
        FreezableBuffer<String> buffer = new FreezableBuffer<>(2);
        for (int i = 1; i <= 5; i++) {
            buffer.append("v" + i); // 2/4 触发两次封冻
        }
        assertThat(buffer.frozenCount()).isEqualTo(2);
        List<List<String>> drained = buffer.drainFrozen();
        assertThat(drained).hasSize(2);
        assertThat(drained.get(0)).containsExactly("v1", "v2");
        assertThat(drained.get(1)).containsExactly("v3", "v4");
        assertThat(buffer.frozenCount()).isZero();      // 取走后清空
        assertThat(buffer.mutableSize()).isEqualTo(1);  // v5 可变段保留
        assertThat(buffer.totalSize()).isEqualTo(1);
        assertThat(buffer.drainFrozen()).isEmpty();     // 再 drain 空
    }

    @Test
    void snapshotShouldFollowAppendOrder() {
        FreezableBuffer<Integer> buffer = new FreezableBuffer<>(2);
        for (int i = 1; i <= 7; i++) {
            buffer.append(i);
        }
        assertThat(buffer.snapshot()).containsExactly(1, 2, 3, 4, 5, 6, 7); // 追加序
    }

    @Test
    void drainThenSnapshotShouldOnlyContainPostDrainAppends() {
        FreezableBuffer<String> buffer = new FreezableBuffer<>(2);
        buffer.append("old1");
        buffer.append("old2");
        buffer.append("old3"); // 冻 old1/old2
        buffer.drainFrozen();
        buffer.append("new1");
        assertThat(buffer.snapshot()).containsExactly("old3", "new1"); // 冻结已取走
    }

    @Test
    void singleCapacityShouldFreezeEveryAppend() {
        FreezableBuffer<String> buffer = new FreezableBuffer<>(1);
        buffer.append("x");
        buffer.append("y"); // 容量 1——立即封冻 x
        assertThat(buffer.frozenCount()).isEqualTo(1);
        assertThat(buffer.mutableSize()).isEqualTo(1);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new FreezableBuffer<String>(0))
                .isInstanceOf(IllegalArgumentException.class);
        FreezableBuffer<String> buffer = new FreezableBuffer<>(2);
        assertThatThrownBy(() -> buffer.append(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
