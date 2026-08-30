package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 197 / T570：模型调用循环闸回归——阈值内计数 / 超限拦 / 隔轮独立 /
 * afterTurn 清零 / null 安全 / 参数校验。
 */
class ModelCallCapHookTest {

    /** 最小 ModelCallContext 桩（beforeModel 只读 sessionId/turn）。 */
    private static final class Call implements ModelCallContext {
        private final HookEnvironment env;
        private final int turn;

        Call(HookEnvironment env, int turn) {
            this.env = env;
            this.turn = turn;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return turn; }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) { }
        @Override public ChatClientRequest request() { return null; }
        @Override public org.springframework.ai.chat.client.ChatClientResponse response() { return null; }
        @Override public Throwable error() { return null; }
        @Override public void replaceRequest(ChatClientRequest newRequest) { }
        @Override public void replaceResponse(
                org.springframework.ai.chat.client.ChatClientResponse newResponse) { }
    }

    @Test
    void countsWithinCapAndBlocksBeyond() {
        ModelCallCapHook hook = new ModelCallCapHook(3);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        List<HookResult> verdicts = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 5; i++) {
            verdicts.add(hook.beforeModel(new Call(env, 1)));
        }

        assertThat(verdicts.subList(0, 3)).allMatch(r -> r == HookResult.CONTINUE);
        assertThat(verdicts.subList(3, 5))
                .allMatch(r -> r instanceof HookResult.Block);
        HookResult.Block block = (HookResult.Block) verdicts.get(3);
        assertThat(block.reason()).contains("上限").contains("4 次");
        assertThat(hook.currentCount("s1", 1)).isEqualTo(5); // 被拒也计（环路仍在发生）
    }

    @Test
    void nextTurnCountsIndependently() {
        ModelCallCapHook hook = new ModelCallCapHook(2);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());

        hook.beforeModel(new Call(env, 1));
        hook.beforeModel(new Call(env, 1));
        assertThat(hook.beforeModel(new Call(env, 1))).isInstanceOf(HookResult.Block.class);

        assertThat(hook.beforeModel(new Call(env, 2))).isEqualTo(HookResult.CONTINUE); // 新轮新账
        assertThat(hook.currentCount("s1", 2)).isEqualTo(1);
    }

    @Test
    void afterTurnClearsCounter() {
        ModelCallCapHook hook = new ModelCallCapHook(2);
        HookEnvironment env = new HookEnvironment("s1", "a", new InMemorySessionStateStore());
        env.nextTurn(); // 推进到轮 1——DefaultTurnContext 的 turn() 读 env.currentTurn()
        int turn = env.currentTurn();
        hook.beforeModel(new Call(env, turn));

        hook.afterTurn(new DefaultTurnContext(env, "done"));
        assertThat(hook.currentCount("s1", turn)).isZero(); // 主动清

        // 清后同轮再调从头计（异常路径不死锁）
        assertThat(hook.beforeModel(new Call(env, turn))).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void sessionsAreIsolated() {
        ModelCallCapHook hook = new ModelCallCapHook(1);
        HookEnvironment a = new HookEnvironment("a", "agent", new InMemorySessionStateStore());
        HookEnvironment b = new HookEnvironment("b", "agent", new InMemorySessionStateStore());

        assertThat(hook.beforeModel(new Call(a, 1))).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.beforeModel(new Call(a, 1))).isInstanceOf(HookResult.Block.class);
        assertThat(hook.beforeModel(new Call(b, 1))).isEqualTo(HookResult.CONTINUE); // 不连坐
    }

    @Test
    void nullContextsAreSafe() {
        ModelCallCapHook hook = new ModelCallCapHook(1);
        assertThat(hook.beforeModel(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.afterTurn(null)).isEqualTo(HookResult.CONTINUE);
        assertThat(hook.currentCount("ghost", 1)).isZero();
    }

    @Test
    void capValidated() {
        assertThatThrownBy(() -> new ModelCallCapHook(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
