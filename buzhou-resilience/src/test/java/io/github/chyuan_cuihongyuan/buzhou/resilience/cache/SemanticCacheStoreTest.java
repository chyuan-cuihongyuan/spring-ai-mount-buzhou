package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 55 §A / T240 / impl-191：语义缓存存储单元级——cosine 判定/阈值边界/桶隔离/
 * LRU 逐出/TTL 惰性过期/零向量与维度错配防御/构造校验。
 */
class SemanticCacheStoreTest {

    private static final org.springframework.ai.chat.model.ChatResponse RESP =
            responseOf("答案");

    private static org.springframework.ai.chat.model.ChatResponse responseOf(String text) {
        return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(
                new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage(text))));
    }

    @Test
    void nearestNeighborHitsAboveThresholdAndMissesBelow() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofHours(1), 0.9);
        store.put("b1", new float[]{1, 0}, RESP);
        // 查询与条目 cosine=1 → 命中
        assertThat(store.findNearest("b1", new float[]{1, 0})).isPresent();
        // cosine=0（正交）→ 低于阈值 miss
        assertThat(store.findNearest("b1", new float[]{0, 1})).isEmpty();
        // 多条目取最近邻：与 (0,1) 更近的条目胜出
        store.put("b1", new float[]{0, 1}, responseOf("另一答案"));
        Optional<org.springframework.ai.chat.model.ChatResponse> nearest =
                store.findNearest("b1", new float[]{0.1f, 0.9f});
        assertThat(nearest).isPresent();
        assertThat(nearest.get().getResult().getOutput().getText()).isEqualTo("另一答案");
        assertThat(store.hitCount()).isEqualTo(2);
        assertThat(store.missCount()).isEqualTo(1);
    }

    @Test
    void thresholdIsInclusiveAtBoundary() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofHours(1), 0.5);
        store.put("b", new float[]{1, 0}, RESP);
        // cosine 恰 = 0.5（(1,0)·(1,√3)/(|..|)= 0.5）→ ≥ 阈值命中（钉住 ≥ 语义）
        double half = Math.sqrt(3);
        assertThat(store.findNearest("b", new float[]{1f, (float) half})).isPresent();
    }

    @Test
    void bucketsIsolateEntries() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofHours(1), 0.5);
        store.put("model-a", new float[]{1, 0}, RESP);
        // 同向量不同桶 → 不比较（跨模型隔离）
        assertThat(store.findNearest("model-b", new float[]{1, 0})).isEmpty();
    }

    @Test
    void ttlExpiresLazilyAndCountsEvicted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-17T00:00:00Z"));
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofSeconds(10), 0.9, clock);
        store.put("b", new float[]{1, 0}, RESP);
        assertThat(store.findNearest("b", new float[]{1, 0})).isPresent();
        // 时间推进越过 TTL → 惰性过期：miss + evicted 计数
        clock.set(Instant.parse("2026-08-17T00:00:30Z"));
        assertThat(store.findNearest("b", new float[]{1, 0})).isEmpty();
        assertThat(store.evictedCount()).isEqualTo(1);
        assertThat(store.size()).isZero();
    }

    /** 可变 Clock（TTL 惰性过期的零等待推进）。 */
    private static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void zeroVectorAndDimensionMismatchAreDefensiveMisses() {
        SemanticCacheStore store = new SemanticCacheStore(16, Duration.ofHours(1), 0.5);
        store.put("b", new float[]{1, 0}, RESP);
        // 零向量查询 → cosine 0（不 NaN）→ miss
        assertThat(store.findNearest("b", new float[]{0, 0})).isEmpty();
        // 维度错配 → 防御跳过 → miss
        assertThat(store.findNearest("b", new float[]{1, 0, 0})).isEmpty();
        assertThat(SemanticCacheStore.cosine(new float[]{1, 0}, new float[]{0, 1, 1})).isZero();
    }

    @Test
    void lruCapacityEvictsEldestAndCounts() {
        SemanticCacheStore store = new SemanticCacheStore(2, Duration.ofHours(1), 0.9);
        store.put("b", new float[]{1, 0}, responseOf("1"));
        store.put("b", new float[]{0, 1}, responseOf("2"));
        store.put("b", new float[]{1, 1}, responseOf("3")); // 容量 2 → 逐出最旧
        assertThat(store.size()).isEqualTo(2);
        assertThat(store.evictedCount()).isEqualTo(1);
        // 最旧条目（(1,0)）已逐出：其精确方向查询 miss；仍在的 (1,1) 命中
        assertThat(store.findNearest("b", new float[]{1, 0})).isEmpty();
        assertThat(store.findNearest("b", new float[]{1, 1})).isPresent();
    }

    @Test
    void constructorValidatesArguments() {
        assertThatThrownBy(() -> new SemanticCacheStore(0, Duration.ofHours(1), 0.9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SemanticCacheStore(16, Duration.ZERO, 0.9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SemanticCacheStore(16, Duration.ofHours(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SemanticCacheStore(16, Duration.ofHours(1), 1.2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
