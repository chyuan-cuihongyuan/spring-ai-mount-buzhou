package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 161 / T520：特征抽取回归——四点累积 / 比率派生零 NaN / LRU 逐出 /
 * 隔离 / 空会话零值行。
 */
class SessionFeaturesTest {

    @Test
    void accumulatesFromAllHookPoints() {
        SessionFeaturesHook hook = new SessionFeaturesHook(new SessionFeatureStore());
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        hook.beforeTurn(new DefaultTurnContext(env, "q1"));        // turns=1
        DefaultToolCallContext ok = new DefaultToolCallContext(env, "t1", "search", Map.of());
        ok.markExecuted("结果", null);
        hook.afterTool(ok);                                        // toolCalls=1
        DefaultToolCallContext bad = new DefaultToolCallContext(env, "t2", "fetch", Map.of());
        bad.markExecuted("[工具执行失败]\n原因：503", null);
        hook.afterTool(bad);                                       // toolCalls=2, toolErrors=1
        hook.onModelError(null);                                   // null 安全
        hook.onModelError(new FailedModelCall(env));               // modelErrors=1

        SessionFeatureStore.Features features = hook.store().features("s1");
        assertThat(features.turns()).isEqualTo(1);
        assertThat(features.toolCalls()).isEqualTo(2);
        assertThat(features.toolErrors()).isEqualTo(1);
        assertThat(features.modelErrors()).isEqualTo(1);
        assertThat(features.lastActiveAt()).isNotNull();
        assertThat(features.toolErrorRate()).isEqualTo(0.5);
        assertThat(features.modelErrorRate()).isEqualTo(1.0);
    }

    @Test
    void ratiosAreZeroNotNaNWhenNoCalls() {
        SessionFeatureStore store = new SessionFeatureStore();
        SessionFeatureStore.Features empty = store.features("ghost");
        assertThat(empty.turns()).isZero();
        assertThat(empty.toolErrorRate()).isZero();
        assertThat(empty.modelErrorRate()).isZero();
        assertThat(empty.lastActiveAt()).isNull();
    }

    @Test
    void lruEvictsLeastRecentlyActive() {
        SessionFeatureStore store = new SessionFeatureStore();
        for (int i = 0; i < 1025; i++) {
            store.recordTurnStart("s" + i);
        }
        // s0 最久未活跃被逐出；s1 仍在
        assertThat(store.features("s0").turns()).isZero();
        assertThat(store.features("s1").turns()).isEqualTo(1);
        assertThat(store.size()).isEqualTo(1024);
    }

    @Test
    void sessionsAreIsolated() {
        SessionFeatureStore store = new SessionFeatureStore();
        store.recordTurnStart("a");
        store.recordToolCall("a", true);
        store.recordTurnStart("b");

        assertThat(store.features("a").toolCalls()).isEqualTo(1);
        assertThat(store.features("b").toolCalls()).isZero();
        assertThat(store.snapshot()).containsOnlyKeys("a", "b");
    }

    @Test
    void hookWithoutStoreSelfInitializes() {
        SessionFeaturesHook hook = new SessionFeaturesHook(null);
        HookEnvironment env = new HookEnvironment("s9", "agent", new InMemorySessionStateStore());
        assertThat(hook.beforeTurn(new DefaultTurnContext(env, "q")))
                .isEqualTo(HookResult.CONTINUE);
        assertThat(hook.store().features("s9").turns()).isEqualTo(1);
    }

    @Test
    void nullContextsAreSafe() {
        SessionFeaturesHook hook = new SessionFeaturesHook(new SessionFeatureStore());
        assertThat(hook.beforeTurn(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterTool(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.onModelError(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.store().size()).isZero();
    }

    /** 最小 ModelCallContext 桩（onModelError 只读 sessionId）。 */
    private record FailedModelCall(HookEnvironment env) implements
            io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext {
        @Override
        public String sessionId() {
            return env.sessionId();
        }

        @Override
        public String agentName() {
            return env.agentName();
        }

        @Override
        public int turn() {
            return 1;
        }

        @Override
        public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
            return env.stateHandle();
        }

        @Override
        public void emitEvent(SessionEvent event) {
        }

        @Override
        public org.springframework.ai.chat.client.ChatClientRequest request() {
            return null;
        }

        @Override
        public org.springframework.ai.chat.client.ChatClientResponse response() {
            return null;
        }

        @Override
        public void replaceRequest(org.springframework.ai.chat.client.ChatClientRequest newRequest) {
        }

        @Override
        public void replaceResponse(org.springframework.ai.chat.client.ChatClientResponse newResponse) {
        }

        @Override
        public Throwable error() {
            return new IllegalStateException("boom");
        }
    }
}
