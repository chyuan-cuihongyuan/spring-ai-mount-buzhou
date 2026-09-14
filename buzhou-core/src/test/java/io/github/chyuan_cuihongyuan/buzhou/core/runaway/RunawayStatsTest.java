package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1076 / impl 828：Runaway 预算 hook 判定分布——放行（allowed）、
 * 硬顶阻断（blocked）、禁用跳过（disabledSkips）、守恒恒等式、归零。
 * StubModelCallContext 同 CounterAtomicitySpreadTest。
 */
class RunawayStatsTest {

    private HookEnvironment env;

    @BeforeEach
    void setUp() {
        RunawayHook.resetForTest();
        env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    private RunawayHook hookWithMaxSteps(int maxSteps) {
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(maxSteps, null, null),
                null, null, null, null, null);
        return new RunawayHook(props, new RunawayCounters());
    }

    private ModelCallContext ctx() {
        return new ModelCallContext() {
            @Override public String sessionId() { return env.sessionId(); }
            @Override public String agentName() { return env.agentName(); }
            @Override public int turn() { return 1; }
            @Override public SessionStateHandle state() { return env.stateHandle(); }
            @Override public void emitEvent(SessionEvent event) { }
            @Override public ChatClientRequest request() { return null; }
            @Override public ChatClientResponse response() { return null; }
            @Override public Throwable error() { return null; }
            @Override public void replaceRequest(ChatClientRequest newRequest) { }
            @Override public void replaceResponse(ChatClientResponse newResponse) { }
        };
    }

    @Test
    void allowedCallsCountAllowed() {
        RunawayHook hook = hookWithMaxSteps(100);
        assertThat(hook.beforeModel(ctx())).isEqualTo(HookResult.CONTINUE);

        RunawayHook.RunawayStats stats = RunawayHook.stats();
        assertThat(stats.invocations()).isEqualTo(1);
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.blocked()).isZero();
    }

    @Test
    void hardStopCountsBlocked() {
        RunawayHook hook = hookWithMaxSteps(1);
        assertThat(hook.beforeModel(ctx())).isEqualTo(HookResult.CONTINUE); // step 1 合法
        assertThat(hook.beforeModel(ctx())).isInstanceOf(HookResult.Block.class); // step 2 超限

        RunawayHook.RunawayStats stats = RunawayHook.stats();
        assertThat(stats.blocked()).isEqualTo(1);
        assertThat(stats.allowed()).isEqualTo(1);
    }

    @Test
    void disabledCountsItsBucket() {
        RunawayHook hook = new RunawayHook(new BuzhouRunawayProperties(false,
                null, null, null, null, null, null), new RunawayCounters());
        assertThat(hook.beforeModel(ctx())).isEqualTo(HookResult.CONTINUE);

        assertThat(RunawayHook.stats().disabledSkips()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHolds() {
        RunawayHook hook = hookWithMaxSteps(2);
        hook.beforeModel(ctx()); // allowed
        hook.beforeModel(ctx()); // allowed
        hook.beforeModel(ctx()); // blocked（step 3 > 2）

        RunawayHook.RunawayStats stats = RunawayHook.stats();
        assertThat(stats.invocations()).isEqualTo(3);
        assertThat(stats.invocations())
                .isEqualTo(stats.blocked() + stats.allowed() + stats.disabledSkips());
        assertThat(stats.allowed()).isEqualTo(2);
        assertThat(stats.blocked()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        RunawayHook hook = hookWithMaxSteps(100);
        hook.beforeModel(ctx());
        assertThat(RunawayHook.stats().invocations()).isEqualTo(1);

        RunawayHook.resetForTest();

        RunawayHook.RunawayStats stats = RunawayHook.stats();
        assertThat(stats.invocations()).isZero();
        assertThat(stats.allowed()).isZero();
        assertThat(stats.blocked()).isZero();
    }
}
