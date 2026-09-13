package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HookChainReplaceStatsTest {

    private final HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    /** 返回指定载荷的 Replace 的钩子。 */
    static class ReplaceHook implements BuzhouHook {
        private final Object payload;

        ReplaceHook(Object payload) {
            this.payload = payload;
        }

        @Override
        public int order() {
            return 100;
        }

        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            return HookResult.replace(payload);
        }
    }

    /** 断言链继续执行的尾随钩子。 */
    static class TailHook implements BuzhouHook {
        final List<String> calls = new ArrayList<>();

        @Override
        public int order() {
            return 200;
        }

        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            calls.add("tail");
            return HookResult.CONTINUE;
        }
    }

    @Test
    void ghostPayloadCountedDroppedAndChainContinues() {
        TailHook tail = new TailHook();
        HookChain chain = HookChain.of(List.of(new ReplaceHook(42), tail));

        HookResult result = chain.beforeTurn(new DefaultTurnContext(env, "q"));

        assertThat(result).isSameAs(HookResult.CONTINUE);
        assertThat(chain.replaceDroppedCount()).isEqualTo(1);
        assertThat(chain.replaceAppliedCount()).isZero();
        assertThat(tail.calls).containsExactly("tail");
    }

    @Test
    void legalStringPayloadCountedApplied() {
        TailHook tail = new TailHook();
        HookChain chain = HookChain.of(List.of(new ReplaceHook("new-input"), tail));

        HookResult result = chain.beforeTurn(new DefaultTurnContext(env, "q"));

        assertThat(result).isSameAs(HookResult.CONTINUE);
        assertThat(chain.replaceAppliedCount()).isEqualTo(1);
        assertThat(chain.replaceDroppedCount()).isZero();
        assertThat(tail.calls).containsExactly("tail");
    }

    @Test
    void mixedTurnsAccumulateBothCounters() {
        TailHook tail = new TailHook();
        HookChain chain = HookChain.of(List.of(new ReplaceHook(42), tail));

        chain.beforeTurn(new DefaultTurnContext(env, "q1"));
        chain.beforeTurn(new DefaultTurnContext(env, "q2"));

        assertThat(chain.replaceDroppedCount()).isEqualTo(2);
        assertThat(chain.replaceAppliedCount()).isZero();
        assertThat(tail.calls).hasSize(2);
    }
}
