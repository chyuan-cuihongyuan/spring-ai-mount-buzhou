package io.github.chyuan_cuihongyuan.buzhou.guard.decision;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2010 / T3122：判定决策缓存合同——TTL 内命中、过期惰性清除且
 * 不计 miss、LRU 驱逐、显式失效、四计数与命中率、畸形 fail-fast。
 */
class DecisionCacheTest {

    @Test
    void withinTtlShouldHitSameVerdict() {
        DecisionCache<String, String> cache = new DecisionCache<>(1_000L, 16);
        cache.put("tool:rm", "ALLOW", 0);
        assertThat(cache.get("tool:rm", 500L)).contains("ALLOW");
        assertThat(cache.get("tool:rm", 999L)).contains("ALLOW"); // 界内（<ttl）
        DecisionCache.CacheStats stats = cache.stats();
        assertThat(stats.hits()).isEqualTo(2L);
        assertThat(stats.misses()).isZero();
    }

    @Test
    void expiredEntryShouldBeLazilyEvictedAndCounted() {
        DecisionCache<String, String> cache = new DecisionCache<>(1_000L, 16);
        cache.put("tool:rm", "ALLOW", 0);
        assertThat(cache.get("tool:rm", 1_000L)).isEmpty(); // 恰过期（>=ttl）
        assertThat(cache.size()).isZero(); // 惰性清除
        DecisionCache.CacheStats stats = cache.stats();
        assertThat(stats.expirations()).isEqualTo(1L);
        assertThat(stats.misses()).isZero(); // 过期非未见过
        // 再查（已清除）→ miss
        assertThat(cache.get("tool:rm", 1_100L)).isEmpty();
        assertThat(cache.stats().misses()).isEqualTo(1L);
    }

    @Test
    void lruEvictionShouldDropLeastRecentlyUsed() {
        DecisionCache<String, String> cache = new DecisionCache<>(10_000L, 2);
        cache.put("a", "ALLOW", 0);
        cache.put("b", "DENY", 0);
        cache.get("a", 1); // a 变为最近使用
        cache.put("c", "ALLOW", 2); // 超容量 → 驱逐 b（最久未用）
        assertThat(cache.get("b", 3)).isEmpty(); // b 被驱逐 → miss
        assertThat(cache.get("a", 3)).contains("ALLOW"); // a 仍在
        assertThat(cache.get("c", 3)).contains("ALLOW");
        assertThat(cache.stats().evictions()).isEqualTo(1L);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void invalidateShouldRemoveImmediately() {
        DecisionCache<String, String> cache = new DecisionCache<>(10_000L, 16);
        cache.put("tool:x", "DENY", 0);
        cache.invalidate("tool:x");
        assertThat(cache.get("tool:x", 1)).isEmpty();
        assertThat(cache.stats().misses()).isEqualTo(1L); // 失效后=未见过
    }

    @Test
    void overwrittenPutShouldRefreshVerdictAndTimestamp() {
        DecisionCache<String, String> cache = new DecisionCache<>(1_000L, 16);
        cache.put("tool:x", "DENY", 0);
        cache.put("tool:x", "ALLOW", 800); // 重判回填刷新时间戳
        assertThat(cache.get("tool:x", 1_500L)).contains("ALLOW"); // 旧 ts 已过期但新 ts 未
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void hitRateShouldReflectDistribution() {
        DecisionCache<String, String> cache = new DecisionCache<>(10_000L, 16);
        cache.put("k", "v", 0);
        cache.get("k", 1);      // hit
        cache.get("k", 2);      // hit
        cache.get("other", 3);  // miss
        DecisionCache.CacheStats stats = cache.stats();
        assertThat(stats.hitRate()).isCloseTo(2.0d / 3.0d,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(new DecisionCache<String, String>(1L, 1).stats().hitRate()).isZero(); // 空不除零
    }

    @Test
    void clockRollbackGetShouldStayLenient() {
        DecisionCache<String, String> cache = new DecisionCache<>(1_000L, 16);
        cache.put("k", "v", 5_000);
        assertThat(cache.get("k", 4_000)).contains("v"); // 回拨宽进（now−ts<0<ttl）
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new DecisionCache<String, String>(0, 16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionCache<String, String>(1_000L, 0))
                .isInstanceOf(IllegalArgumentException.class);
        DecisionCache<String, String> cache = new DecisionCache<>(1_000L, 16);
        assertThatThrownBy(() -> cache.get(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(null, "v", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put("k", null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.invalidate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
