package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HookChainTest {

    private final HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

    static class RecordingHook implements BuzhouHook {
        final List<String> calls = new ArrayList<>();
        private final int order;

        RecordingHook(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public HookResult beforeTool(ToolCallContext ctx) {
            calls.add(name() + ":" + ctx.toolName());
            return HookResult.CONTINUE;
        }
    }

    @Test
    void hooksRunInOrderAscending() {
        RecordingHook late = new RecordingHook(1000);
        RecordingHook early = new RecordingHook(100);
        HookChain chain = new HookChain(List.of(late, early), Set.of());

        chain.beforeTool(new DefaultToolCallContext(env, "tc1", "tool_a", Map.of()));

        assertThat(early.calls).hasSize(1);
        assertThat(late.calls).hasSize(1);
        assertThat(chain.hooks()).containsExactly(early, late);
    }

    @Test
    void disabledHookIsSkipped() {
        RecordingHook hook = new RecordingHook(100);
        HookChain chain = new HookChain(List.of(hook), Set.of(hook.name()));

        chain.beforeTool(new DefaultToolCallContext(env, "tc1", "tool_a", Map.of()));

        assertThat(hook.calls).isEmpty();
    }

    @Test
    void blockStopsSubsequentHooks() {
        BuzhouHook blocker = new BuzhouHook() {
            @Override
            public int order() {
                return 100;
            }

            @Override
            public HookResult beforeTool(ToolCallContext ctx) {
                return HookResult.block("denied");
            }
        };
        RecordingHook after = new RecordingHook(200);
        HookChain chain = new HookChain(List.of(blocker, after), Set.of());

        HookResult result = chain.beforeTool(new DefaultToolCallContext(env, "tc1", "tool_a", Map.of()));

        assertThat(result).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) result).reason()).isEqualTo("denied");
        assertThat(after.calls).isEmpty();
    }

    @Test
    void replaceUpdatesArgumentsForSubsequentHooks() {
        BuzhouHook replacer = new BuzhouHook() {
            @Override
            public int order() {
                return 100;
            }

            @Override
            public HookResult beforeTool(ToolCallContext ctx) {
                return HookResult.replace(Map.of("sql", "SELECT 1"));
            }
        };
        AtomicInteger seenArgs = new AtomicInteger();
        BuzhouHook observer = new BuzhouHook() {
            @Override
            public int order() {
                return 200;
            }

            @Override
            public HookResult beforeTool(ToolCallContext ctx) {
                if (ctx.arguments().containsKey("sql")) {
                    seenArgs.incrementAndGet();
                }
                return HookResult.CONTINUE;
            }
        };
        HookChain chain = new HookChain(List.of(replacer, observer), Set.of());
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc1", "run_sql", Map.of());

        chain.beforeTool(ctx);

        assertThat(seenArgs.get()).isEqualTo(1);
        assertThat(ctx.arguments()).containsEntry("sql", "SELECT 1");
    }

    @Test
    void onEventIsPureNotification() {
        AtomicInteger events = new AtomicInteger();
        BuzhouHook listener = new BuzhouHook() {
            @Override
            public void onEvent(SessionEventContext ctx) {
                events.incrementAndGet();
            }
        };
        HookChain chain = HookChain.of(List.of(listener));

        chain.fireEvent(new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook
                .DefaultSessionEventContext(env, SessionEvent.of("test")));

        assertThat(events.get()).isEqualTo(1);
    }

    @Test
    void beforeTurnReplaceInputFlowsThrough() {
        BuzhouHook replacer = new BuzhouHook() {
            @Override
            public HookResult beforeTurn(TurnContext ctx) {
                return HookResult.replace("rewritten input");
            }
        };
        HookChain chain = HookChain.of(List.of(replacer));
        DefaultTurnContext ctx = new DefaultTurnContext(env, "original");

        chain.beforeTurn(ctx);

        assertThat(ctx.input()).isEqualTo("rewritten input");
    }
}
