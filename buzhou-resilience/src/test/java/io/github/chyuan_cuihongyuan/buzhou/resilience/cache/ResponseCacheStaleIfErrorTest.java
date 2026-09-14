package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 响应缓存 stale-if-error 测试（spec 1604 / T2359–T2360 / impl 1157）：
 * 宽限窗内过期条目保留可救场（getStale 命中 + staleReads 计数）、超窗照弃、
 * advisor 失败路径救场不抛、无救场条目异常照抛、默认关零变化。
 * Varnish grace / RFC 5861 思想。
 */
class ResponseCacheStaleIfErrorTest {

    static final class MutableClock extends Clock {
        private volatile Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private static ChatResponse responseOf(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void staleWithinGraceWindowIsReadableAndCounted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofMinutes(10), 0,
                Duration.ofMinutes(5), clock);
        store.put("k", responseOf("旧答案"));
        clock.advance(Duration.ofMinutes(11)); // 过期 1 分钟（宽限 5 分钟内）
        assertThat(store.get("k")).isEmpty();            // 正常读 miss
        assertThat(store.size()).isEqualTo(1);           // 条目保留（救场机会）
        assertThat(store.getStale("k")).hasValueSatisfying(r ->
                assertThat(r.getResult().getOutput().getText()).isEqualTo("旧答案"));
        assertThat(store.staleReadCount()).isEqualTo(1);
        clock.advance(Duration.ofMinutes(5));            // 总过期 6 分钟 > 宽限 5 分钟
        assertThat(store.getStale("k")).isEmpty();       // 超窗不可救
        assertThat(store.get("k")).isEmpty();
        assertThat(store.size()).isZero();                // 超窗后条目被弃
    }

    @Test
    void defaultZeroWindowKeepsDiscardSemantics() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofMinutes(1), clock);
        store.put("k", responseOf("旧答案"));
        clock.advance(Duration.ofMinutes(2));
        assertThat(store.get("k")).isEmpty();
        assertThat(store.getStale("k")).isEmpty();       // 关闭态恒不可救
        assertThat(store.size()).isZero();                // 过期即弃（零变化）
    }

    /** 失败即抛的伪链（救场路径验证用）。 */
    private static final class FailingChain implements CallAdvisorChain {
        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            throw new IllegalStateException("模型全挂");
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
            return List.of();
        }

        @Override
        public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
            return this;
        }
    }

    private static ChatClientRequest requestOf(String text) {
        return new ChatClientRequest(new Prompt(new UserMessage(text)), java.util.Map.of());
    }

    @Test
    void advisorServesStaleOnModelFailureInsteadOfThrowing() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofMinutes(10), 0,
                Duration.ofMinutes(5), clock);
        ResponseCacheAdvisor advisor = new ResponseCacheAdvisor(store, "m");
        ChatClientRequest request = requestOf("问题");
        String key = ResponseCacheKeys.keyOf("m", request.prompt());
        store.put(key, responseOf("旧答案"));
        clock.advance(Duration.ofMinutes(12)); // 过期 2 分钟（宽限内）
        assertThat(store.get(key)).isEmpty(); // 先走一次 miss（确认过期）
        ChatClientResponse out = advisor.adviseCall(request, new FailingChain());
        assertThat(out.chatResponse().getResult().getOutput().getText()).isEqualTo("旧答案");
        assertThat(store.staleReadCount()).isEqualTo(1);
    }

    @Test
    void advisorRethrowsWhenNoStaleRescueAvailable() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-15T00:00:00Z"));
        ResponseCacheStore store = new ResponseCacheStore(8, Duration.ofMinutes(10), clock);
        ResponseCacheAdvisor advisor = new ResponseCacheAdvisor(store, "m");
        ChatClientRequest request = requestOf("问题");
        assertThatThrownBy(() -> advisor.adviseCall(request, new FailingChain()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("模型全挂"); // 失败语义不静默吞
        assertThat(store.staleReadCount()).isZero();
    }
}
