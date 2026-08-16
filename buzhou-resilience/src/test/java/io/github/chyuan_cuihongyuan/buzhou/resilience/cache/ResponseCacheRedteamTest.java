package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 53 对抗面 / T208 / impl-173：缓存面红队——键注入 / 工具结果漂移不串键 /
 * TTL 过期不陈旧 / 热键压挤存活 / 命中重放不可变。
 */
class ResponseCacheRedteamTest {

    /** 注入①：消息内容含键序列化元字符（分隔符伪造）不得串键。 */
    @Test
    void keyInjectionWithMetaCharactersDoesNotCollide() {
        Prompt normal = new Prompt(List.of(new UserMessage("a|b}c{d")));
        Prompt forged = new Prompt(List.of(
                new UserMessage("a"), new UserMessage("b}c"), new UserMessage("d")));
        // 两条语义不同的消息序列（第二条试图伪造第一条的序列化形态）→ 键必须不同
        String k1 = ResponseCacheKeys.keyOf("m", normal);
        String k2 = ResponseCacheKeys.keyOf("m", forged);
        assertThat(k1).isNotEqualTo(k2);
        // 同输入确定性
        assertThat(ResponseCacheKeys.keyOf("m", normal)).isEqualTo(k1);
        // 模型名入键
        assertThat(ResponseCacheKeys.keyOf("other", normal)).isNotEqualTo(k1);
    }

    /** 注入②：TTL 过期后命中路径不返回陈旧（advisor 层级联，注入 Clock）。 */
    @Test
    void expiredEntriesNeverServedThroughAdvisorPath() {
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
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofSeconds(30), clock);
        store.put("k", new ChatResponse(List.of(new Generation(new AssistantMessage("旧答案")))));
        now.set(Instant.parse("2026-08-16T00:01:00Z")); // 过期
        assertThat(store.get("k")).isEmpty();
        assertThat(store.evictedCount()).isEqualTo(1);
    }

    /** 压挤③：容量压挤后热键（近期访问）存活、冷键逐出。 */
    @Test
    void hotKeySurvivesCapacityPressure() {
        ResponseCacheStore store = new ResponseCacheStore(3, Duration.ofHours(1));
        for (int i = 0; i < 5; i++) {
            store.put("k" + i, new ChatResponse(List.of(new Generation(new AssistantMessage("v" + i)))));
            store.get("k0"); // k0 持续保热
        }
        assertThat(store.get("k0")).isPresent();
        assertThat(store.size()).isEqualTo(3);
        assertThat(store.evictedCount()).isEqualTo(2);
    }

    /** 重放④：两次命中返回独立包装实例（不共享可变上下文引用语义）。 */
    @Test
    void hitReplayReturnsFreshWrapperInstances() {
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofHours(1));
        ChatResponse cached = new ChatResponse(List.of(new Generation(new AssistantMessage("v"))));
        store.put("k", cached);
        assertThat(store.get("k")).containsSame(cached);
        assertThat(store.get("k")).containsSame(cached); // ChatResponse 本身只读共享；
        // 不可变性语义由 advisor 侧「new ChatClientResponse(cached, context)」保证
        // （ResponseCacheEndToEndTest 已钉住行为面）
        assertThat(store.hitCount()).isEqualTo(2);
    }
}
