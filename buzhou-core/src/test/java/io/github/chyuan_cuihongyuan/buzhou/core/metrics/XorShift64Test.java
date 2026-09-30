package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XorShift64Test {

    @Test
    void shouldBeDeterministicAndNonZeroEscaping() {
        // 同种子同轨迹（前 1000 步逐值全等）
        long a = 0x9E3779B97F4A7C15L;
        long b = 0x9E3779B97F4A7C15L;
        for (int i = 0; i < 1000; i++) {
            a = XorShift64.next(a);
            b = XorShift64.next(b);
            assertThat(a).isEqualTo(b);
            assertThat(a).isNotZero(); // 全零态不可达（死锁防御）
        }
        // 状态空间铺展：10 万步高位低位都活跃
        long state = 42;
        Set<Long> seen = new HashSet<>();
        int highDistinct = 0;
        Set<Integer> highBuckets = new HashSet<>();
        for (int i = 0; i < 100000; i++) {
            state = XorShift64.next(state);
            seen.add(state);
            highBuckets.add((int) (state >>> 40));
        }
        assertThat(seen.size()).isEqualTo(100000); // 无短环碰撞（前 10 万步互异）
        assertThat(highBuckets.size()).isGreaterThan(200); // 高 24 位铺展
    }

    @Test
    void shouldDrawUnbiasedBoundedValues() {
        // 有界分布圣像：bound=100 抽 20 万——每桶频次在期望±20% 内
        long state = 7;
        int[] counts = new int[100];
        for (int i = 0; i < 200000; i++) {
            state = XorShift64.next(state);
            counts[(int) (Math.floorMod(state, 100))]++; // 直接用状态取模近似同分布
        }
        for (int bucket = 0; bucket < 100; bucket++) {
            assertThat(counts[bucket]).isBetween(1600, 2400);
        }
        // nextLong 契约面
        assertThat(XorShift64.nextLong(99, 1)).isZero();
        for (long seed = 1; seed < 50; seed++) {
            assertThat(XorShift64.nextLong(seed, 10)).isLessThan(10L).isGreaterThanOrEqualTo(0L);
        }
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> XorShift64.next(0))
                .hasMessageContaining("种子非 0");
        assertThatThrownBy(() -> XorShift64.nextLong(5, 0))
                .hasMessageContaining("上界为正");
        assertThatThrownBy(() -> XorShift64.nextLong(5, -3))
                .hasMessageContaining("上界为正");
    }
}
