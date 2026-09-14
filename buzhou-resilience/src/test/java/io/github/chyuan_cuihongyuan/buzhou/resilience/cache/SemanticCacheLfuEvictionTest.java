package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1600 / T2351–T2352 / impl 1153：LFU 采样驱逐行为面——默认关零变化 /
 * opt-in 热条目保护 / 命中计数封顶（纯函数）/ 权重腾挪同款采样 / 负参 fail-fast。
 *
 * <p>分化场景钉死（accessOrder LRU 下触达即变 recent，热条目保护只有在其为 eldest
 * 时才与纯 LRU 分化）：put A → 命中 A 若干 → put B（A 变 eldest 且高频）→ put C 触驱逐。
 */
class SemanticCacheLfuEvictionTest {

    private static final String BUCKET = "b";
    private static final float[] EMBEDDING_A = {1, 0};
    /** 与 EMBEDDING_A cosine=0（正交）：独立第二条目，不干扰 A 的最近邻命中。 */
    private static final float[] EMBEDDING_B = {0, 1};

    private static org.springframework.ai.chat.model.ChatResponse responseOf(String text) {
        return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(
                new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage(text))));
    }

    /** 命中 A 指定次数（findNearest 同向量 cosine=1 ≥ 阈值 0.9）。 */
    private static void hitA(SemanticCacheStore store, int times) {
        for (int i = 0; i < times; i++) {
            assertThat(store.findNearest(BUCKET, EMBEDDING_A)).isPresent();
        }
    }

    @Test
    void defaultSamplingOffKeepsPureLruZeroChange() {
        SemanticCacheStore store = new SemanticCacheStore(2, Duration.ofHours(1), 0.9);
        store.put(BUCKET, EMBEDDING_A, responseOf("热答案"));
        hitA(store, 3);
        store.put(BUCKET, EMBEDDING_B, responseOf("冷答案"));
        // 触发容量驱逐：A 是 eldest（B 刚写入、A 最后命中早于 B 写入）→ 纯 LRU 逐 A
        store.put(BUCKET, new float[]{1, 1}, responseOf("新答案"));
        assertThat(store.size()).isEqualTo(2);
        // A 出局（驱逐后只剩 B 与新条目——A 查询 miss）
        assertThat(store.findNearest(BUCKET, EMBEDDING_A)).isEmpty();
        assertThat(store.hotPreservedCount()).isZero();
    }

    @Test
    void samplingEvictsLowHitVictimAndPreservesHotEldest() {
        SemanticCacheStore store = new SemanticCacheStore(2, Duration.ofHours(1), 0.9, 0, 2,
                java.time.Clock.systemUTC());
        store.put(BUCKET, EMBEDDING_A, responseOf("热答案"));
        hitA(store, 3);
        store.put(BUCKET, EMBEDDING_B, responseOf("冷答案"));
        // 采样窗口 [A(hits=3, eldest), B(hits=0)] → victim=B：高频 eldest 被保护
        store.put(BUCKET, new float[]{1, 1}, responseOf("新答案"));
        assertThat(store.size()).isEqualTo(2);
        assertThat(store.findNearest(BUCKET, EMBEDDING_A)).isPresent();
        assertThat(store.findNearest(BUCKET, EMBEDDING_B)).isEmpty();
        assertThat(store.hotPreservedCount()).isEqualTo(1);
    }

    @Test
    void hitCountBumpsAreCappedByCeiling() {
        assertThat(SemanticCacheStore.bumpHitCount(0)).isEqualTo(1);
        assertThat(SemanticCacheStore.bumpHitCount(999)).isEqualTo(1_000);
        // 封顶后不再增长（线性计数够用——TTL 兜底衰减，spec 1600 推演注）
        assertThat(SemanticCacheStore.bumpHitCount(1_000)).isEqualTo(1_000);
        assertThat(SemanticCacheStore.bumpHitCount(5_000)).isEqualTo(1_000);
    }

    @Test
    void weightBudgetEvictionUsesSameSamplingSemantics() {
        // 权重预算仅容 2 条（每条 10 字符 × 预算 25）；容量上限放大到不触发
        SemanticCacheStore store = new SemanticCacheStore(64, Duration.ofHours(1), 0.9, 25, 2,
                java.time.Clock.systemUTC());
        store.put(BUCKET, EMBEDDING_A, responseOf("0123456789"));
        hitA(store, 3);
        store.put(BUCKET, EMBEDDING_B, responseOf("1234567890"));
        // 腾挪触发：20+10>25，采样窗口 [A(3), B(0)] → victim=B（默认路径会逐 eldest A）
        store.put(BUCKET, new float[]{1, 1}, responseOf("2345678901"));
        assertThat(store.findNearest(BUCKET, EMBEDDING_A)).isPresent();
        assertThat(store.findNearest(BUCKET, EMBEDDING_B)).isEmpty();
        assertThat(store.weightEvictionCount()).isEqualTo(1);
        assertThat(store.hotPreservedCount()).isEqualTo(1);
        assertThat(store.totalWeightChars()).isEqualTo(20);
    }

    @Test
    void negativeSampleSizeFailsFastInStoreAndProperties() {
        assertThatThrownBy(() -> new SemanticCacheStore(2, Duration.ofHours(1), 0.9, 0, -1,
                java.time.Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eviction-sample-size");
        assertThatThrownBy(() -> new io.github.chyuan_cuihongyuan.buzhou.resilience.config
                .ResilienceProperties.SemanticCache(
                        Boolean.TRUE, 0.9, 16, Duration.ofHours(1), 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eviction-sample-size");
    }
}
