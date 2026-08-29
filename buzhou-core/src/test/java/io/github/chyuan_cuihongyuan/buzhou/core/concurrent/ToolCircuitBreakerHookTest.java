package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 131 / T478：工具级熔断回归——失败率跳闸 / 成功为主不跳 / 冷却后半开恢复 /
 * 半开一败重开 / 半开超额拒 / hook 标记记结局（纯 hook 驱动到跳闸）。
 */
class ToolCircuitBreakerHookTest {

    /** 快测配置：窗 4 / 阈值 50% / 冷却 200ms / 半开 2。 */
    private static ToolCircuitBreaker.Config fastConfig() {
        return new ToolCircuitBreaker.Config(4, 50.0, Duration.ofMillis(200), 2);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-08-30T00:00:00Z");

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
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
    void failureRateTripsOpenAndBlocks() {
        MutableClock clock = new MutableClock();
        ToolCircuitBreaker breaker = new ToolCircuitBreaker(fastConfig(), clock);

        breaker.recordFailure("flaky");
        breaker.recordFailure("flaky");
        breaker.recordSuccess("flaky");
        assertThat(breaker.stateOf("flaky").state()).isEqualTo(ToolCircuitBreaker.State.CLOSED);
        breaker.recordSuccess("flaky"); // 窗满 4，失败率 50% ≥ 阈值 → OPEN

        ToolCircuitBreaker.View view = breaker.stateOf("flaky");
        assertThat(view.state()).isEqualTo(ToolCircuitBreaker.State.OPEN);
        assertThat(breaker.tryAcquirePermission("flaky")).isFalse();
        assertThat(breaker.stateOf("flaky").blocked()).isEqualTo(1);
    }

    @Test
    void mostlySuccessStaysClosed() {
        ToolCircuitBreaker breaker = new ToolCircuitBreaker(fastConfig(), new MutableClock());
        breaker.recordSuccess("ok");
        breaker.recordSuccess("ok");
        breaker.recordFailure("ok");
        breaker.recordSuccess("ok"); // 失败率 25% < 50%
        assertThat(breaker.stateOf("ok").state()).isEqualTo(ToolCircuitBreaker.State.CLOSED);
        assertThat(breaker.tryAcquirePermission("ok")).isTrue();
    }

    @Test
    void cooldownThenHalfOpenRecovers() {
        MutableClock clock = new MutableClock();
        ToolCircuitBreaker breaker = new ToolCircuitBreaker(fastConfig(), clock);
        for (int i = 0; i < 4; i++) {
            breaker.recordFailure("flaky");
        }
        assertThat(breaker.stateOf("flaky").state()).isEqualTo(ToolCircuitBreaker.State.OPEN);

        clock.advanceMillis(250);
        assertThat(breaker.tryAcquirePermission("flaky")).isTrue(); // 冷却耗尽 → 半开放行
        assertThat(breaker.stateOf("flaky").state()).isEqualTo(ToolCircuitBreaker.State.HALF_OPEN);
        assertThat(breaker.tryAcquirePermission("flaky")).isTrue(); // 第 2 个探测名额
        assertThat(breaker.tryAcquirePermission("flaky")).isFalse(); // 超额拒
        breaker.recordSuccess("flaky");
        breaker.recordSuccess("flaky"); // 探测全成 → CLOSED 清窗

        ToolCircuitBreaker.View view = breaker.stateOf("flaky");
        assertThat(view.state()).isEqualTo(ToolCircuitBreaker.State.CLOSED);
        assertThat(view.windowSuccess()).isZero();
        assertThat(breaker.tryAcquirePermission("flaky")).isTrue();
    }

    @Test
    void halfOpenFailureReopensWithFreshCooldown() {
        MutableClock clock = new MutableClock();
        ToolCircuitBreaker breaker = new ToolCircuitBreaker(fastConfig(), clock);
        for (int i = 0; i < 4; i++) {
            breaker.recordFailure("flaky");
        }
        clock.advanceMillis(250);
        assertThat(breaker.tryAcquirePermission("flaky")).isTrue();
        breaker.recordFailure("flaky"); // 半开一败即重开

        assertThat(breaker.stateOf("flaky").state()).isEqualTo(ToolCircuitBreaker.State.OPEN);
        assertThat(breaker.tryAcquirePermission("flaky")).isFalse();
        clock.advanceMillis(250); // 新冷却耗尽，可再探测
        assertThat(breaker.tryAcquirePermission("flaky")).isTrue();
    }

    @Test
    void hookDrivesBreakerToOpenViaStructuredMarkers() {
        MutableClock clock = new MutableClock();
        ToolCircuitBreakerHook hook = new ToolCircuitBreakerHook(
                new ToolCircuitBreaker(fastConfig(), clock));
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        // 4 次错误反馈（结构化标记）→ 窗满失败率 100% → 跳闸
        for (int i = 0; i < 4; i++) {
            DefaultToolCallContext ctx =
                    new DefaultToolCallContext(env, "tc" + i, "flaky", Map.of());
            ctx.markExecuted("[工具执行失败]\n工具：flaky\n原因：下游 503", null);
            assertThat(hook.afterTool(ctx)).isEqualTo(HookResult.CONTINUE);
        }
        assertThat(hook.breaker().stateOf("flaky").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);

        // beforeTool 拒：block 文案可读（含冷却提示），模型可改道
        DefaultToolCallContext rejected =
                new DefaultToolCallContext(env, "tc9", "flaky", Map.of());
        HookResult verdict = hook.beforeTool(rejected);
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason()).contains("熔断");

        // 正常结果记成功；校验失败标记也计败
        DefaultToolCallContext fine =
                new DefaultToolCallContext(env, "tc10", "steady", Map.of());
        fine.markExecuted("正常结果", null);
        hook.afterTool(fine);
        assertThat(hook.breaker().stateOf("steady").windowSuccess()).isEqualTo(1);

        DefaultToolCallContext invalid =
                new DefaultToolCallContext(env, "tc11", "steady", Map.of());
        invalid.markExecuted("[工具参数校验失败]（参数未过 schema，工具未执行）", null);
        hook.afterTool(invalid);
        assertThat(hook.breaker().stateOf("steady").windowFailure()).isEqualTo(1);
    }

    @Test
    void configValidatedFailFast() {
        assertThatThrownBy(() -> new ToolCircuitBreaker.Config(1, 50,
                Duration.ofSeconds(1), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolCircuitBreaker.Config(4, 0,
                Duration.ofSeconds(1), 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolCircuitBreaker.Config(4, 50,
                Duration.ZERO, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolCircuitBreaker.Config(4, 50,
                Duration.ofSeconds(1), 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
