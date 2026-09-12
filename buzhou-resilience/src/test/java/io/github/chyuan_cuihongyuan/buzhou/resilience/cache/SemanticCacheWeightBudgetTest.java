package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 701 / T1002–T1003：语义缓存权重预算驱逐（Caffeine weigher 思想）——
 * 腾挪序/超预算拒存/默认 0 零行为/权重回收与 readout 一致性。
 */
class SemanticCacheWeightBudgetTest {

    private static org.springframework.ai.chat.model.ChatResponse responseOf(String text) {
        return new org.springframework.ai.chat.model.ChatResponse(List.of(
                new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage(text))));
    }

    private static SemanticCacheStore store(long budget) {
        return new SemanticCacheStore(64, Duration.ofHours(1), 0.9, budget,
                Clock.systemUTC());
    }

    @Test
    void evictsEldestUntilIncomingFitsBudget() {
        SemanticCacheStore store = store(100);
        store.put("b", new float[]{1, 0}, responseOf("x".repeat(60)));
        assertThat(store.totalWeightChars()).isEqualTo(60);
        store.put("b", new float[]{0.9f, 0.1f}, responseOf("y".repeat(60)));
        assertThat(store.size()).isEqualTo(1); // 60+60>100 → eldest 腾挪
        assertThat(store.weightEvictionCount()).isEqualTo(1);
        assertThat(store.totalWeightChars()).isEqualTo(60);
        assertThat(store.evictedCount()).isZero(); // 口径分离：不混 LRU/TTL 计数
    }

    @Test
    void oversizedEntryIsRejectedNotStored() {
        SemanticCacheStore store = store(50);
        store.put("b", new float[]{1, 0}, responseOf("z".repeat(80)));
        assertThat(store.size()).isZero();
        assertThat(store.totalWeightChars()).isZero();
        assertThat(store.weightEvictionCount()).isEqualTo(1);
        // 未存 → 查询必 miss（不计 hit）
        assertThat(store.findNearest("b", new float[]{1, 0})).isEmpty();
    }

    @Test
    void disabledBudgetKeepsLegacyBehaviorExactly() {
        SemanticCacheStore store = store(0);
        store.put("b", new float[]{1, 0}, responseOf("x".repeat(10_000)));
        store.put("b", new float[]{0.9f, 0.1f}, responseOf("y".repeat(10_000)));
        assertThat(store.size()).isEqualTo(2); // 不腾挪不限重
        assertThat(store.weightEvictionCount()).isZero();
        assertThat(store.maxWeightChars()).isZero();
        assertThat(store.totalWeightChars()).isZero(); // 关闭时不追踪
    }

    @Test
    void ttlPurgeReclaimsWeightAndReadoutsStayConsistent() {
        MutableClock clock = new MutableClock();
        SemanticCacheStore store = new SemanticCacheStore(64, Duration.ofSeconds(10), 0.9, 100, clock);
        store.put("b", new float[]{1, 0}, responseOf("x".repeat(40)));
        clock.advanceSeconds(11);
        assertThat(store.size()).isZero(); // 惰性过期
        assertThat(store.totalWeightChars()).isZero(); // 权重同步回收
        assertThat(store.evictedCount()).isEqualTo(1);
        assertThat(store.weightEvictionCount()).isZero(); // TTL 不算权重驱逐
    }

    @Test
    void weightSumsAcrossGenerations() {
        org.springframework.ai.chat.model.ChatResponse multi =
                new org.springframework.ai.chat.model.ChatResponse(List.of(
                        new org.springframework.ai.chat.model.Generation(
                                new org.springframework.ai.chat.messages.AssistantMessage("12345")),
                        new org.springframework.ai.chat.model.Generation(
                                new org.springframework.ai.chat.messages.AssistantMessage("678"))));
        assertThat(SemanticCacheStore.estimateChars(multi)).isEqualTo(8);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-12T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
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
}
