package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BitonicSorterTest {

    @Test
    void shouldSortClassicAnchors() {
        // 手锚：8 元素双调清洁
        long[] values = {3, 7, 8, 12, 15, 9, 5, 1}; // 升半+降半双调
        BitonicSorter.sort(values);
        assertThat(values).isSorted().containsExactly(1L, 3L, 5L, 7L, 8L, 9L, 12L, 15L);
        // 已序/逆序/全等退化
        long[] sorted = {1, 2, 3, 4};
        BitonicSorter.sort(sorted);
        assertThat(sorted).isSorted();
        long[] reversed = {4, 3, 2, 1};
        BitonicSorter.sort(reversed);
        assertThat(reversed).containsExactly(1L, 2L, 3L, 4L);
        long[] equal = {7, 7, 7, 7};
        BitonicSorter.sort(equal);
        assertThat(equal).containsExactly(7L, 7L, 7L, 7L);
        assertThat(BitonicSorter.sortedCopy(new long[]{9, 1, 8, 2})).containsExactly(1L, 2L, 8L, 9L);
    }

    @Test
    void shouldMatchArraysSortOnRandomPowers() {
        // 圣像：随机 2 的幂尺寸 vs Arrays.sort 全等（含负数/重复）
        Random random = new Random(61);
        for (int power = 1; power <= 12; power++) {
            int n = 1 << power;
            for (int t = 0; t < 5; t++) {
                long[] values = new long[n];
                if (t % 2 == 0) {
                    for (int i = 0; i < n; i++) {
                        values[i] = random.nextLong();
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        values[i] = random.nextInt(10); // 高重复
                    }
                }
                long[] expected = values.clone();
                Arrays.sort(expected);
                BitonicSorter.sort(values);
                assertThat(values).as("n=%d 第 %d 组", n, t).containsExactly(expected);
            }
        }
    }

    @Test
    void shouldCarryNetworkInvariantsAndFailFast() {
        // 网络读数锚：n=8 → 比较器数 (k=3) 3+4·1+2·2+1·3? 主项 n/4·(k²+k)=8/4·12=24? 锚 = 6+6+4·3/2 …固定值校验
        // Batcher 网络真值（非最优网络——4 输入最优 5 但双调用 6）
        assertThat(BitonicSorter.comparatorCount(2)).isEqualTo(1);
        assertThat(BitonicSorter.comparatorCount(4)).isEqualTo(6);
        assertThat(BitonicSorter.comparatorCount(8)).isEqualTo(24);
        assertThat(BitonicSorter.comparatorCount(16)).isEqualTo(80);
        assertThatThrownBy(() -> BitonicSorter.sort(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitonicSorter.sort(new long[]{}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitonicSorter.sort(new long[]{3, 1, 2}))
                .hasMessageContaining("2 的幂");
        assertThatThrownBy(() -> BitonicSorter.comparatorCount(6))
                .hasMessageContaining("2 的幂");
    }
}
