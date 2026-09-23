package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5015 / T6132：SIEVE 合同——命中置位不重排、清位跳过、
 * 分叉场景、容量 1 边界、畸形 fail-fast。
 */
class SieveCacheTest {

    @Test
    void hitShouldMarkVisitedWithoutReordering() {
        SieveCache<String, String> cache = new SieveCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("a");
        assertThat(cache.order()).containsExactly("a", "b", "c");   // 命中零重排
        assertThat(cache.get("a")).isEqualTo("1");
    }

    @Test
    void visitedEntryShouldBeSkippedOnceThenEvictable() {
        SieveCache<String, String> cache = new SieveCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("a");            // a 置位
        cache.put("d", "4");       // 满：a 清位跳过 → 驱逐 b
        assertThat(cache.containsKey("b")).isFalse();
        assertThat(cache.containsKey("a")).isTrue();   // a 靠保护位存活
        assertThat(cache.order()).containsExactly("a", "c", "d");
    }

    @Test
    void oldestVisitedEntryShouldSurviveAcrossInserts() {
        SieveCache<String, String> cache = new SieveCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("a");            // a 置位
        cache.put("d", "4");       // 驱逐 b（a 清位跳过）
        cache.get("c");            // c 置位
        cache.put("e", "5");       // c 清位跳过 → 驱逐 d
        assertThat(cache.containsKey("d")).isFalse();
        assertThat(cache.containsKey("a")).isTrue();   // 最老但已访问——跨插入存活（LRU 必先逐出）
        assertThat(cache.containsKey("c")).isTrue();
        assertThat(cache.order()).containsExactly("a", "c", "e");
    }

    @Test
    void capacityOneShouldEvictOnEveryNewInsert() {
        SieveCache<String, String> cache = new SieveCache<>(1);
        cache.put("a", "1");
        cache.put("b", "2");       // a 访问位未置——直接驱逐
        assertThat(cache.containsKey("a")).isFalse();
        assertThat(cache.get("b")).isEqualTo("2");
        cache.put("c", "3");       // b 已置位（get）——清位跳过后… 容量 1 环回自身再驱逐
        assertThat(cache.containsKey("b")).isFalse();
        assertThat(cache.containsKey("c")).isTrue();
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> new SieveCache<>(0)).isInstanceOf(IllegalArgumentException.class);
        SieveCache<String, String> cache = new SieveCache<>(2);
        assertThatThrownBy(() -> cache.put(null, "v")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.put("k", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.get(null)).isInstanceOf(NullPointerException.class);
    }
}
