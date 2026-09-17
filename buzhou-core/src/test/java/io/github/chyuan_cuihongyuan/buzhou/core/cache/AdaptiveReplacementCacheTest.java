package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3009 / T5020：ARC 合同——命中/未命中、容量守恒、同键更新
 * 不增条目、新近晋升保护、幽灵 B1 命中 p 上调（0→1 手迹）、扫描
 * 抗性（40 键一次性扫描后热键 9/10 存活——LRU 全灭对照）、p 有界、
 * 幽灵链有界、容量校验。
 */
class AdaptiveReplacementCacheTest {

    @Test
    void missShouldReturnNullAndHitShouldReturn() {
        AdaptiveReplacementCache<String, Integer> cache = new AdaptiveReplacementCache<>(4);
        assertThat(cache.get("a")).isNull();
        cache.put("a", 1);
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void sizeShouldStayWithinCapacity() {
        AdaptiveReplacementCache<Integer, String> cache = new AdaptiveReplacementCache<>(5);
        for (int i = 0; i < 50; i++) {
            cache.put(i, "v" + i);
            assertThat(cache.size()).isLessThanOrEqualTo(5);
        }
        assertThat(cache.size()).isEqualTo(5);
    }

    @Test
    void updatingExistingKeyShouldNotGrow() {
        AdaptiveReplacementCache<String, Integer> cache = new AdaptiveReplacementCache<>(3);
        cache.put("a", 1);
        cache.put("a", 2);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.get("a")).isEqualTo(2);
    }

    @Test
    void recencyPromotionShouldProtectJustHitKey() {
        // c=2：put a,b → get(a)（a 晋升 T2）→ put c 挤掉 b（LRU）而非 a
        AdaptiveReplacementCache<String, Integer> cache = new AdaptiveReplacementCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        assertThat(cache.get("a")).isEqualTo(1);
        cache.put("c", 3);
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("b")).isNull();
    }

    @Test
    void ghostB1HitShouldAdaptTargetUp() {
        // c=3 手迹：put+get 1（晋升 T2）；put 2,3,4——put 4 时 total=3 达容量
        // REPLACE 逐 2 入 B1；put 2（B1 幽灵命中）→ p: 0→1，2 进 T2
        // （REPLACE 逐 T1-LRU 3 入 B1；T2={1,2} T1={4} 恰满）
        AdaptiveReplacementCache<Integer, Integer> cache = new AdaptiveReplacementCache<>(3);
        cache.put(1, 1);
        cache.get(1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.put(4, 4);
        assertThat(cache.b1Size()).isEqualTo(1);
        assertThat(cache.targetRecency()).isZero();
        cache.put(2, 2);
        assertThat(cache.targetRecency()).isEqualTo(1);
        assertThat(cache.t2Size()).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    void oneTimeScanShouldNotFlushHotKeys() {
        // c=10：warm 1..10（get 晋升 T2）→ 40 键一次性扫描（put）→
        // 热键 ≥9 存活（LRU 单链口径 10 键全灭——扫描抗性）
        AdaptiveReplacementCache<Integer, Integer> cache = new AdaptiveReplacementCache<>(10);
        for (int i = 1; i <= 10; i++) {
            cache.put(i, i);
            cache.get(i);
        }
        assertThat(cache.t2Size()).isEqualTo(10);
        for (int i = 11; i <= 50; i++) {
            cache.put(i, i);
        }
        int survived = 0;
        for (int i = 1; i <= 10; i++) {
            if (cache.get(i) != null) {
                survived++;
            }
        }
        assertThat(survived).isGreaterThanOrEqualTo(9);
        assertThat(cache.size()).isLessThanOrEqualTo(10);
    }

    @Test
    void targetShouldStayWithinZeroToCapacity() {
        AdaptiveReplacementCache<Integer, Integer> cache = new AdaptiveReplacementCache<>(4);
        java.util.random.RandomGenerator rng =
                java.util.random.RandomGeneratorFactory.of("L64X256MixRandom").create(31);
        for (int i = 0; i < 500; i++) {
            int key = rng.nextInt(30);
            if (cache.get(key) == null) {
                cache.put(key, key);
            }
            assertThat(cache.targetRecency()).isBetween(0, 4);
        }
    }

    @Test
    void ghostListsShouldStayBounded() {
        AdaptiveReplacementCache<Integer, Integer> cache = new AdaptiveReplacementCache<>(6);
        for (int i = 0; i < 100; i++) {
            cache.put(i, i);
            assertThat(cache.b1Size()).isLessThanOrEqualTo(6);
            assertThat(cache.b2Size()).isLessThanOrEqualTo(6);
        }
    }

    @Test
    void capacityMustBePositive() {
        assertThatThrownBy(() -> new AdaptiveReplacementCache<String, Integer>(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveReplacementCache<String, Integer>(-3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
