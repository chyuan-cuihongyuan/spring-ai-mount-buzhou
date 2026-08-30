package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 126 §B / T453：提示前缀缓存红队——同前缀命中（续命 + 计数）；异前缀
 * miss；LRU 封顶逐出诚实计数（命中率不被逐出污染）；getOrLoad 装载一次后续
 * 命中；keyOf 规范形白名单 fail-fast；无请求 hitRate=0 诚实。借鉴：vLLM/SGLang
 * radix prefix-cache。
 */
class PromptPrefixCacheTest {

    @Test
    void identicalPrefixHitsAndCountsHonestMisses() {
        PromptPrefixCache<String> cache = PromptPrefixCache.create();
        String k = PromptPrefixCache.keyOf("system: 你是运维助手\nrules: v3");

        assertThat(cache.get(k)).isEmpty(); // miss
        cache.put(k, "parsed-v3");
        assertThat(cache.get(k)).contains("parsed-v3"); // hit
        assertThat(cache.get(PromptPrefixCache.keyOf("system: 你是运维助手\nrules: v4")))
                .isEmpty(); // 异前缀 miss——本缓存不猜语义相似

        PromptPrefixCache.Stats stats = cache.stats();
        assertThat(stats.requests()).isEqualTo(3);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(2);
        assertThat(stats.hitRate()).isCloseTo(1d / 3, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void lruBoundEvictsOldestUnusedAndCounts() {
        PromptPrefixCache<String> cache = PromptPrefixCache.create(2);
        String k1 = PromptPrefixCache.keyOf("prefix-one");
        String k2 = PromptPrefixCache.keyOf("prefix-two");
        String k3 = PromptPrefixCache.keyOf("prefix-three");
        cache.put(k1, "v1");
        cache.put(k2, "v2");
        cache.get(k1); // k1 续命——下一个逐出应是 k2

        cache.put(k3, "v3"); // 容量 2：逐出 k2

        assertThat(cache.get(k2)).isEmpty();
        assertThat(cache.get(k1)).contains("v1");
        assertThat(cache.get(k3)).contains("v3");
        assertThat(cache.stats().evictions()).isEqualTo(1);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void getOrLoadLoadsOnceThenHits() {
        PromptPrefixCache<Integer> cache = PromptPrefixCache.create();
        String k = PromptPrefixCache.keyOf("embed:doc-42");
        int[] loads = {0};

        Optional<Integer> first = cache.getOrLoad(k, () -> {
            loads[0]++;
            return 7;
        });
        Optional<Integer> second = cache.getOrLoad(k, () -> {
            loads[0]++;
            return 9; // 不应被调用
        });

        assertThat(first).contains(7);
        assertThat(second).contains(7);
        assertThat(loads[0]).isEqualTo(1);
        assertThat(cache.stats().hits()).isEqualTo(1);
        assertThat(cache.stats().misses()).isEqualTo(1);
    }

    @Test
    void keyAndValueValidationFailFast() {
        assertThatThrownBy(() -> PromptPrefixCache.keyOf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptPrefixCache.keyOf(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PromptPrefixCache.create(0))
                .isInstanceOf(IllegalArgumentException.class);
        PromptPrefixCache<String> cache = PromptPrefixCache.create();
        assertThatThrownBy(() -> cache.put("k", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noRequestsHitRateIsZeroHonest() {
        PromptPrefixCache<String> cache = PromptPrefixCache.create();
        assertThat(cache.stats().hitRate()).isZero();
        assertThat(cache.stats().requests()).isZero();
        assertThat(cache.size()).isZero();
    }
}
