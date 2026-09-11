package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 缓存命中率便利 getter 测试（spec 639 / T928–T929 / impl 492）：
 * Response/Semantic 两 store 的 hitRate（0..1；零请求诚实 0.0）。
 */
class CacheHitRateTest {

    private static final ChatResponse STUB =
            new ChatResponse(List.of());

    /** ResponseCacheStore：3 中 1 失 → 0.75；零请求 → 0.0。 */
    @Test
    void responseCacheHitRate() {
        ResponseCacheStore store = new ResponseCacheStore(16, Duration.ofMinutes(1));
        assertThat(store.hitRate()).isZero();

        store.put("k1", STUB);
        store.put("k2", STUB);
        store.put("k3", STUB);
        store.get("k1");
        store.get("k2");
        store.get("k3");
        store.get("gone");

        assertThat(store.hitRate()).isCloseTo(0.75, within(1e-9));
    }

    /** SemanticCacheStore：桶内最近邻命中口径同 hitRate。 */
    @Test
    void semanticCacheHitRate() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofMinutes(1), 0.9);
        assertThat(store.hitRate()).isZero();

        store.put("b", new float[] {1f, 0f}, STUB);
        store.findNearest("b", new float[] {1f, 0f}); // hit
        store.findNearest("b", new float[] {0f, 1f}); // miss（cos=0 < 0.9）

        assertThat(store.hitRate()).isCloseTo(0.5, within(1e-9));
    }
}
