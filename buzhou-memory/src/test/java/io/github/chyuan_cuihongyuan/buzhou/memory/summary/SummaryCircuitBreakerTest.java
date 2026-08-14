package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 13 §stores-7 / ticket 32：摘要熔断半开恢复契约——
 * 失败 N 次开闸 → 窗口后半开试探一次 → 成功清零恢复 / 失败重计重新关窗；
 * 计数随会话清理与闲置淘汰兜底。时钟可注入（确定性驱动窗口流逝）。
 */
class SummaryCircuitBreakerTest {

    private static final Duration WINDOW = Duration.ofMinutes(10);

    /** 可推进的测试时钟（默认窗口 PT10M 内无需真实等待）。 */
    private static final class MutableClock extends Clock {

        private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void shouldOpenCircuit_whenConsecutiveFailuresReachThreshold() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-open";

        assertThat(breaker.allows(sessionId)).isTrue();
        for (int i = 0; i < SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD; i++) {
            breaker.onFailure(sessionId);
        }
        assertThat(breaker.allows(sessionId)).isFalse(); // 达阈值开闸
    }

    @Test
    void shouldStayOpen_whenFailureWindowNotElapsed() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-stay-open";
        openBreaker(breaker, sessionId);

        clock.advance(WINDOW.minusSeconds(1)); // 窗口差一秒未过
        assertThat(breaker.allows(sessionId)).isFalse();
    }

    @Test
    void shouldAllowSingleProbe_whenFailureWindowElapsed() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-probe";
        openBreaker(breaker, sessionId);

        clock.advance(WINDOW.plusSeconds(1));
        assertThat(breaker.allows(sessionId)).isTrue();  // 半开：放行一次试探
        assertThat(breaker.allows(sessionId)).isFalse(); // 探针在途：其余请求仍拒绝
    }

    @Test
    void shouldRecoverToClosed_whenProbeSucceedsAfterWindow() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-recover";
        openBreaker(breaker, sessionId);

        clock.advance(WINDOW.plusSeconds(1));
        assertThat(breaker.allows(sessionId)).isTrue(); // 试探放行
        breaker.onSuccess(sessionId);                   // 试探成功 → 计数清零

        assertThat(breaker.allows(sessionId)).isTrue(); // 回到闭合态正常放行
        assertThat(breaker.trackedSessions()).isZero(); // 成功即清理（无残留计数）
    }

    @Test
    void shouldRemainOpen_whenProbeFailsAfterWindow() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-probe-fail";
        openBreaker(breaker, sessionId);

        clock.advance(WINDOW.plusSeconds(1));
        assertThat(breaker.allows(sessionId)).isTrue(); // 试探放行
        breaker.onFailure(sessionId);                   // 试探失败 → 重计 + 重新关窗

        assertThat(breaker.allows(sessionId)).isFalse(); // 窗口内继续关
        clock.advance(WINDOW.plusSeconds(1));
        assertThat(breaker.allows(sessionId)).isTrue();  // 下一窗口再放试探
    }

    @Test
    void shouldClearSessionCounters_whenSessionRemoved() {
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW);
        String sessionId = "s-remove";
        breaker.onFailure(sessionId);
        breaker.onFailure(sessionId);
        assertThat(breaker.trackedSessions()).isEqualTo(1);

        breaker.removeSession(sessionId); // 会话关闭级联清理入口

        assertThat(breaker.trackedSessions()).isZero();
        assertThat(breaker.allows(sessionId)).isTrue(); // 计数随清理归零
    }

    @Test
    void shouldEvictStaleSessionStates_whenIdleBeyondWindow() {
        MutableClock clock = new MutableClock();
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(
                SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD, WINDOW, clock);
        String sessionId = "s-stale";
        breaker.onFailure(sessionId); // 半途失败条目（未开闸）
        clock.advance(WINDOW.plusSeconds(1));

        // 惰性淘汰按扫描间隔触发：跑足间隔轮次后闲置条目被清扫
        //（allows 对健康会话不建条目，driver 会话本身不产生残留）
        for (int i = 0; i < 2 * SummaryCircuitBreaker.EVICT_SCAN_INTERVAL; i++) {
            breaker.allows("probe-driver");
        }
        assertThat(breaker.trackedSessions()).isZero();
    }

    private void openBreaker(SummaryCircuitBreaker breaker, String sessionId) {
        for (int i = 0; i < SummaryCircuitBreaker.DEFAULT_FAILURE_THRESHOLD; i++) {
            breaker.onFailure(sessionId);
        }
        assertThat(breaker.allows(sessionId)).isFalse();
    }
}
