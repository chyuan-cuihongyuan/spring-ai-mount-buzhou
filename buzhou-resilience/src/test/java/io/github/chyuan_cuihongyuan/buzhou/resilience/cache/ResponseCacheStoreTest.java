package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 53 §D / T206：LRU 容量 + TTL 惰性过期（可注入 Clock）+ 逐出计数。
 */
class ResponseCacheStoreTest {

    private static ChatResponse resp(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void ttlLazilyExpiresOnRead() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-08-16T00:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public Instant instant() {
                return now.get();
            }

            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }
        };
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofMinutes(10), clock);
        store.put("k", resp("v"));
        assertThat(store.get("k")).isPresent();

        // 时间推进 11 分钟 → 惰性过期（miss + evicted 计数，不返回陈旧）
        now.set(Instant.parse("2026-08-16T00:11:00Z"));
        assertThat(store.get("k")).isEmpty();
        assertThat(store.evictedCount()).isEqualTo(1);
        assertThat(store.missCount()).isEqualTo(1);
        assertThat(store.size()).isZero();
    }

    @Test
    void lruEvictsEldestAndCounts() {
        ResponseCacheStore store = new ResponseCacheStore(2, Duration.ofHours(1));
        store.put("a", resp("a"));
        store.put("b", resp("b"));
        store.get("a"); // a 变最新 → b 最老
        store.put("c", resp("c")); // 容量 2 → 逐出 b
        assertThat(store.get("a")).isPresent();
        assertThat(store.get("b")).isEmpty();
        assertThat(store.get("c")).isPresent();
        assertThat(store.evictedCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidConfig() {
        assertThatThrownBy(() -> new ResponseCacheStore(0, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-entries");
        assertThatThrownBy(() -> new ResponseCacheStore(8, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl");
    }
}
