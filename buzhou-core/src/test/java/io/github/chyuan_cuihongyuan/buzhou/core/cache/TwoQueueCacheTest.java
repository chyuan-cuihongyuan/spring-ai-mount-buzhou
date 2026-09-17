package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3033 / T5068：2Q 合同——命中/未命中/更新不增、总容量守恒、
 * 二触晋升保护（get 过的键在入口冲洗后仍存活）、扫描抗性（20 键
 * 一次性扫描后主区 5 键全存活——LRU 全灭对照）、双逐出计数对账、
 * 参数校验。
 */
class TwoQueueCacheTest {

    @Test
    void hitMissAndUpdateBasics() {
        TwoQueueCache<String, Integer> cache = new TwoQueueCache<>(4, 2);
        assertThat(cache.get("a")).isNull();
        cache.put("a", 1);
        assertThat(cache.get("a")).isEqualTo(1);
        cache.put("a", 2);   // 入口内更新即晋升
        assertThat(cache.get("a")).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.capacity()).isEqualTo(4);
    }

    @Test
    void sizeShouldStayWithinCapacity() {
        TwoQueueCache<Integer, Integer> cache = new TwoQueueCache<>(4, 2);
        for (int i = 0; i < 50; i++) {
            cache.put(i, i);
            assertThat(cache.size()).isLessThanOrEqualTo(4);
        }
    }

    @Test
    void secondTouchShouldPromoteAndProtect() {
        // c=4 in=2：a,b 入口 → get a 晋升主区 → c,d 冲洗入口
        // → a 仍存活（主区），b 已被 FIFO 淘汰
        TwoQueueCache<String, Integer> cache = new TwoQueueCache<>(4, 2);
        cache.put("a", 1);
        cache.put("b", 2);
        assertThat(cache.get("a")).isEqualTo(1);   // 晋升 Am
        cache.put("c", 3);
        cache.put("d", 4);                          // 入口 [c,d]，b 已淘汰
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("b")).isNull();
    }

    @Test
    void scanShouldNotFlushMainRegion() {
        // c=10 in=5：warm 10 键 put+get 逐个晋升——主区容量 5 淘旧留新
        // （终存 6..10）；20 键一次性扫描全在入口自旋；主区 5 键全存活
        TwoQueueCache<Integer, Integer> cache = new TwoQueueCache<>(10, 5);
        for (int i = 1; i <= 10; i++) {
            cache.put(i, i);
            cache.get(i);   // 二触晋升
        }
        for (int scan = 100; scan < 120; scan++) {
            cache.put(scan, scan);
        }
        int survived = 0;
        for (int i = 1; i <= 10; i++) {
            if (cache.get(i) != null) {
                survived++;
            }
        }
        assertThat(survived).isEqualTo(5);   // 主区 6..10 全存活，1..5 早被主区 LRU 淘汰
        assertThat(cache.size()).isLessThanOrEqualTo(10);
    }

    @Test
    void evictionLedgerShouldAccount() {
        TwoQueueCache<Integer, Integer> cache = new TwoQueueCache<>(4, 2);
        for (int i = 0; i < 10; i++) {
            cache.put(i, i);   // 8 次入口淘汰
        }
        assertThat(cache.inboundEvictions()).isEqualTo(8);
        assertThat(cache.mainEvictions()).isZero();
        assertThat(cache.inboundCapacity()).isEqualTo(2);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new TwoQueueCache<String, Integer>(1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TwoQueueCache<String, Integer>(4, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TwoQueueCache<String, Integer>(4, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
