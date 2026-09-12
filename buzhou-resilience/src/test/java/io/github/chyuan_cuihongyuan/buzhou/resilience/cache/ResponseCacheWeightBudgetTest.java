package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 737 / T1074–T1075：响应缓存权重预算（701 同款扩散）——腾挪/拒存/
 * 默认关/替换回收。
 */
class ResponseCacheWeightBudgetTest {

    private static ChatResponse responseOf(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void budgetEvictsEldestAndRejectsOversized() {
        ResponseCacheStore store = new ResponseCacheStore(64, Duration.ofHours(1), 100, Clock.systemUTC());
        store.put("big1", responseOf("x".repeat(60)));
        store.put("big2", responseOf("y".repeat(60))); // 60+60>100 → big1 腾挪
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.weightEvictionCount()).isEqualTo(1);
        assertThat(store.totalWeightChars()).isEqualTo(60);
        assertThat(store.get("big1")).isEmpty(); // 被逐
        assertThat(store.get("big2")).isPresent();

        // 超预算单条拒存
        store.put("huge", responseOf("z".repeat(200)));
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.weightEvictionCount()).isEqualTo(2);
        assertThat(store.maxWeightChars()).isEqualTo(100);
    }

    @Test
    void replaceSameKeyReclaimsOldWeight() {
        ResponseCacheStore store = new ResponseCacheStore(64, Duration.ofHours(1), 100, Clock.systemUTC());
        store.put("k", responseOf("x".repeat(60)));
        store.put("k", responseOf("y".repeat(40))); // 同键替换：先回收 60 再加 40
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.totalWeightChars()).isEqualTo(40);
        assertThat(store.weightEvictionCount()).isZero();
    }

    @Test
    void disabledBudgetKeepsLegacyBehavior() {
        ResponseCacheStore store = new ResponseCacheStore(64, Duration.ofHours(1), 0, Clock.systemUTC());
        store.put("a", responseOf("x".repeat(10_000)));
        store.put("b", responseOf("y".repeat(10_000)));
        assertThat(store.size()).isEqualTo(2);
        assertThat(store.weightEvictionCount()).isZero();
        assertThat(store.totalWeightChars()).isZero();
    }
}
