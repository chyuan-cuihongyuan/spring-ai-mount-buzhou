package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5001 / T6104：Roaring 合同——圣像对拍、密疏容器转换、
 * and/or 守恒、越域 fail-fast、确定性回放。
 */
class RoaringBitSetTest {

    @Test
    void operationSequenceShouldMatchHashSetOracle() {
        Random random = new Random(5001L);
        RoaringBitSet bitmap = new RoaringBitSet();
        Set<Integer> oracle = new HashSet<>();
        for (int op = 0; op < 100_000; op++) {
            int value = random.nextInt(1 << 20);
            if (random.nextBoolean()) {
                bitmap.add(value);
                oracle.add(value);
            } else {
                bitmap.remove(value);
                oracle.remove(value);
            }
            if (op % 10_000 == 0) {
                assertThat(bitmap.cardinality()).isEqualTo(oracle.size());
            }
        }
        assertThat(bitmap.cardinality()).isEqualTo(oracle.size());
        for (int value = 0; value < 1 << 20; value += 97) {
            assertThat(bitmap.contains(value)).isEqualTo(oracle.contains(value));
        }
    }

    @Test
    void containerShouldConvertSparseToDense() {
        RoaringBitSet bitmap = new RoaringBitSet();
        int base = 65535 * 3 + 7;   // 单桶内
        for (int i = 0; i < 4096; i++) {
            bitmap.add(base + i);
        }
        assertThat(bitmap.cardinality()).isEqualTo(4096L);   // 密转位图阈值触发
        for (int i = 0; i < 4096; i++) {
            assertThat(bitmap.contains(base + i)).isTrue();
        }
        for (int i = 0; i < 4096; i++) {
            bitmap.remove(base + i);
        }
        assertThat(bitmap.cardinality()).isZero();
    }

    @Test
    void andShouldIntersectContainers() {
        RoaringBitSet left = new RoaringBitSet();
        RoaringBitSet right = new RoaringBitSet();
        for (int i = 0; i < 5000; i++) {
            left.add(i);
            right.add(i + 2500);
        }
        RoaringBitSet intersected = RoaringBitSet.and(left, right);
        assertThat(intersected.cardinality()).isEqualTo(2500L);
        assertThat(intersected.contains(2500)).isTrue();
        assertThat(intersected.contains(2499)).isFalse();
        assertThat(intersected.contains(4999)).isTrue();
        assertThat(left.cardinality()).isEqualTo(5000L);   // 操作数不变
    }

    @Test
    void orShouldUnionContainers() {
        RoaringBitSet left = new RoaringBitSet();
        RoaringBitSet right = new RoaringBitSet();
        for (int i = 0; i < 3000; i++) {
            left.add(i);
            right.add(i + 1000);
        }
        RoaringBitSet union = RoaringBitSet.or(left, right);
        assertThat(union.cardinality()).isEqualTo(4000L);
        assertThat(union.contains(0)).isTrue();
        assertThat(union.contains(3999)).isTrue();
        assertThat(union.contains(4000)).isFalse();
    }

    @Test
    void negativeAndOutOfRangeShouldFailFast() {
        RoaringBitSet bitmap = new RoaringBitSet();
        assertThatThrownBy(() -> bitmap.add(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bitmap.contains(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bitmap.remove(-7)).isInstanceOf(IllegalArgumentException.class);
        bitmap.add(Integer.MAX_VALUE);   // int 正域边界合法
        assertThat(bitmap.contains(Integer.MAX_VALUE)).isTrue();
    }
}
