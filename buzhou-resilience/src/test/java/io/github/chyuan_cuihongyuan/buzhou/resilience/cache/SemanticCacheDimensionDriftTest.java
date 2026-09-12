package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 语义缓存维度漂移测试（spec 611 / T872–T873 / impl 464，模型漂移可观测思想）：
 * 查询与条目维度不一致 → 计数可见 + 跳过不抛；同维度不受影响；漂移后新条目自然收敛。
 */
class SemanticCacheDimensionDriftTest {

    private static final org.springframework.ai.chat.model.ChatResponse STUB_RESPONSE =
            new org.springframework.ai.chat.model.ChatResponse(java.util.List.of());

    /** 4 维条目缓存 + 3 维查询 → miss + dimensionMismatches 计数，不抛。 */
    @Test
    void dimensionMismatchCountedAndSkipped() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofMinutes(5), 0.9);
        store.put("b", new float[] {1f, 0f, 0f, 0f}, STUB_RESPONSE);

        var result = store.findNearest("b", new float[] {1f, 0f, 0f});

        assertThat(result).isEmpty();          // 维度不匹配跳过（不参与相似度）
        assertThat(store.missCount()).isEqualTo(1);
        assertThat(store.dimensionMismatches()).isEqualTo(1); // 漂移可见
        assertThat(store.findNearest("b", new float[] {1f, 0f, 0f})).isEmpty();
        assertThat(store.dimensionMismatches()).isEqualTo(2); // 持续累计
    }

    /** 同维度查询不受漂移计数影响；命中口径不变。 */
    @Test
    void matchingDimensionStillHits() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofMinutes(5), 0.9);
        store.put("b", new float[] {1f, 0f}, STUB_RESPONSE);

        assertThat(store.findNearest("b", new float[] {1f, 0f})).isPresent();
        assertThat(store.dimensionMismatches()).isZero();
    }

    /** 漂移后写入新维度条目 → 新查询命中（自然收敛——清缓存非必需，旧条目被 LRU/TTL 淘汰）。 */
    @Test
    void newDimensionEntriesConverge() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofMinutes(5), 0.9);
        store.put("b", new float[] {1f, 0f, 0f, 0f}, STUB_RESPONSE); // 旧维度
        store.findNearest("b", new float[] {1f, 0f});                // 触发漂移计数
        store.put("b", new float[] {1f, 0f}, STUB_RESPONSE);         // 新维度条目共存

        assertThat(store.findNearest("b", new float[] {1f, 0f})).isPresent(); // 命中新条目
    }
}
