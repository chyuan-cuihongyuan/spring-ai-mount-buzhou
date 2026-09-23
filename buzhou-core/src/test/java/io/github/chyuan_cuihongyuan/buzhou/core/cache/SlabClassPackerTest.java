package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4015 / T6032：Slab 类装箱合同——几何档表、恰界归档、
 * 浪费比、分配记账、畸形 fail-fast。
 */
class SlabClassPackerTest {

    @Test
    void classTableShouldGrowGeometrically() {
        SlabClassPacker packer = new SlabClassPacker(96, 1024, 1.25);
        assertThat(packer.classCount()).isEqualTo(12);
        assertThat(packer.chunkSizeFor(0)).isEqualTo(96);
        assertThat(packer.chunkSizeFor(1)).isEqualTo(120);
        assertThat(packer.chunkSizeFor(11)).isEqualTo(1024);   // 封顶档
    }

    @Test
    void itemShouldFitSmallestClassWithExactBoundary() {
        SlabClassPacker packer = new SlabClassPacker(96, 1024, 1.25);
        assertThat(packer.classFor(96)).isZero();   // 恰界归本档不进位
        assertThat(packer.classFor(97)).isEqualTo(1);   // 120 档
        assertThat(packer.classFor(121)).isEqualTo(2);   // 150 档
        assertThat(packer.classFor(1024)).isEqualTo(11);
        assertThat(packer.classFor(1025)).isEqualTo(-1);   // 超最大块拒收
    }

    @Test
    void wasteRatioShouldMeasureInternalFragmentation() {
        SlabClassPacker packer = new SlabClassPacker(96, 1024, 1.25);
        assertThat(packer.wasteRatio(96)).isZero();   // 恰合零浪费
        assertThat(packer.wasteRatio(97)).isEqualTo(23.0 / 120);   // 120−97
        assertThat(packer.wasteRatio(1025)).isNaN();   // 拒收诚实
    }

    @Test
    void allocationAccountingShouldTallyPerClass() {
        SlabClassPacker packer = new SlabClassPacker(96, 1024, 1.25);
        assertThat(packer.allocate(96)).isZero();
        assertThat(packer.allocate(100)).isEqualTo(1);
        assertThat(packer.allocate(110)).isEqualTo(1);
        assertThat(packer.allocate(2000)).isEqualTo(-1);   // 拒收不记账
        assertThat(packer.allocationsIn(0)).isEqualTo(1);
        assertThat(packer.allocationsIn(1)).isEqualTo(2);
        assertThat(packer.allocationsIn(11)).isZero();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SlabClassPacker(4, 1024, 1.25))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlabClassPacker(96, 1024, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlabClassPacker(2048, 1024, 1.25))
                .isInstanceOf(IllegalArgumentException.class);
        SlabClassPacker packer = new SlabClassPacker(96, 1024, 1.25);
        assertThatThrownBy(() -> packer.classFor(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> packer.chunkSizeFor(12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> packer.allocationsIn(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
