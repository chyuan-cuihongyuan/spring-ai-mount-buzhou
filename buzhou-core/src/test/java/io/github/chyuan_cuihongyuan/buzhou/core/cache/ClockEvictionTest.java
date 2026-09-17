package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3028 / T5058：CLOCK 合同——命中/未命中、容量守恒、二次机会
 * 手迹（访问过者幸存未访问者被逐）、全引用环扫清位再逐、更新不
 * 增条目、逐出计数对账、近 LRU 性质（刚访问者存活）、容量校验。
 */
class ClockEvictionTest {

    @Test
    void hitAndMissBasics() {
        ClockEviction<String, Integer> cache = new ClockEviction<>(4);
        assertThat(cache.get("a")).isNull();
        cache.put("a", 1);
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void sizeShouldStayWithinCapacity() {
        ClockEviction<Integer, Integer> cache = new ClockEviction<>(3);
        for (int i = 0; i < 100; i++) {
            cache.put(i, i);
            assertThat(cache.size()).isLessThanOrEqualTo(3);
        }
        assertThat(cache.evictedCount()).isEqualTo(97);
    }

    @Test
    void secondChanceShouldProtectAccessedEntries() {
        // 变体 A：新条目带引用位进入。首代全 T 扫一圈清位逐 a
        // （FIFO 样）；第二代位 [d=F? d=T, b=F, c→get 置 T]——
        // 引用过的 c 幸存、未引用的 b 被逐（二次机会显形）
        ClockEviction<String, Integer> cache = new ClockEviction<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4);  // 首代全 T：扫清全位逐 a → [d(T),b(F),c(F)]
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("c")).isEqualTo(3);  // 置位
        cache.put("e", 5);  // b 位 0 被逐，c 位 1 幸存
        assertThat(cache.get("b")).isNull();
        assertThat(cache.get("c")).isEqualTo(3);
        assertThat(cache.get("d")).isEqualTo(4);
        assertThat(cache.get("e")).isEqualTo(5);
    }

    @Test
    void allReferencedRingShouldClearThenEvict() {
        // c=2 双引用位：扫一圈清位后逐首个（a）
        ClockEviction<String, Integer> cache = new ClockEviction<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("b")).isEqualTo(2);
        cache.put("c", 3);
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("b")).isEqualTo(2);
        assertThat(cache.get("c")).isEqualTo(3);
    }

    @Test
    void updateExistingShouldNotGrow() {
        ClockEviction<String, Integer> cache = new ClockEviction<>(2);
        cache.put("k", 1);
        cache.put("k", 2);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.get("k")).isEqualTo(2);
        assertThat(cache.evictedCount()).isZero();
    }

    @Test
    void recentlyAccessedShouldSurviveInsertPressure() {
        // 两代场景：首代扫清后 [4(T),2(F),3(F)]——get 2、get 4 置位
        // → put 5 逐未访问的 3（访问者 2/4 幸存）
        ClockEviction<Integer, Integer> cache = new ClockEviction<>(3);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);  // 首代逐 1 → [4(T),2(F),3(F)] h1
        assertThat(cache.get(1)).isNull();
        assertThat(cache.get(2)).isEqualTo(2);
        assertThat(cache.get(4)).isEqualTo(4);
        cache.put(5, 5);  // 2 清位跳过（二次机会）、3 位 0 被逐
        assertThat(cache.get(2)).isEqualTo(2);
        assertThat(cache.get(4)).isEqualTo(4);
        assertThat(cache.get(3)).isNull();
        assertThat(cache.get(5)).isEqualTo(5);
    }

    @Test
    void capacityMustBePositive() {
        assertThatThrownBy(() -> new ClockEviction<String, Integer>(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClockEviction<String, Integer>(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
