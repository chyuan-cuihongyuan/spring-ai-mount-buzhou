package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FactCollectionStatsTest {

    /** 记账 FakeStore：save 抛错可控。 */
    static final class FakeStore implements FactStore {
        final List<Fact> saved = new ArrayList<>();
        boolean failOnSave;

        @Override
        public void save(String sessionId, Fact fact) {
            if (failOnSave) {
                throw new IllegalStateException("store down");
            }
            saved.add(fact);
        }

        @Override
        public List<Fact> activeFacts(String sessionId, int currentTurn) {
            return List.copyOf(saved);
        }

        @Override
        public void delete(String sessionId, String key) {
        }
    }

    /** 判定器脚手架：可指定命中/抛错。 */
    static FactDefinition def(String name, boolean hit, boolean boom) {
        return new FactDefinition() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<Fact> judge(ToolCallContext ctx) {
                if (boom) {
                    throw new IllegalStateException("判定器坏了");
                }
                return hit ? Optional.of(new Fact("k", "v", name, 0, 1)) : Optional.empty();
            }

            @Override
            public String render(Fact fact) {
                return String.valueOf(fact.value());
            }
        };
    }

    private ToolCallContext ctx() {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext toolCtx = new DefaultToolCallContext(env, "tc-1", "upsert_table", Map.of());
        toolCtx.markExecuted("ok", null);
        return toolCtx;
    }

    @Test
    void happyPathCountsSaved() {
        FakeStore store = new FakeStore();
        FactCollectorHook hook = new FactCollectorHook(
                List.of(def("a", true, false), def("b", true, false)), store);

        hook.afterTool(ctx());

        assertThat(hook.stats()).isEqualTo(new FactCollectorHook.FactCollectionStats(2, 0));
        assertThat(store.saved).hasSize(2);
    }

    @Test
    void judgeFailureIsolatedAndCounted() {
        FakeStore store = new FakeStore();
        FactCollectorHook hook = new FactCollectorHook(
                List.of(def("broken", true, true), def("healthy", true, false)), store);

        hook.afterTool(ctx());

        assertThat(hook.stats()).isEqualTo(new FactCollectorHook.FactCollectionStats(1, 1));
        assertThat(store.saved).hasSize(1); // 健康定义照常采集
    }

    @Test
    void saveFailureCountedNotPropagated() {
        FakeStore store = new FakeStore();
        store.failOnSave = true;
        FactCollectorHook hook = new FactCollectorHook(
                List.of(def("a", true, false)), store);

        hook.afterTool(ctx()); // 不抛——隔离

        assertThat(hook.stats()).isEqualTo(new FactCollectorHook.FactCollectionStats(0, 1));
    }

    @Test
    void emptyVerdictsCountNothing() {
        FakeStore store = new FakeStore();
        FactCollectorHook hook = new FactCollectorHook(
                List.of(def("a", false, false)), store);

        hook.afterTool(ctx());

        assertThat(hook.stats()).isEqualTo(new FactCollectorHook.FactCollectionStats(0, 0));
    }
}
