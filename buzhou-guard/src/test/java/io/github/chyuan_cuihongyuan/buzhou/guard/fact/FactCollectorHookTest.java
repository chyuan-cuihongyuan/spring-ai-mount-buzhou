package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DefaultFactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FactCollector 判定器测试（spec 07：判定器从入参判定语义，非硬匹配工具名）。
 *
 * <p>蓝本例：带 {@code tableId} 的 {@code upsertTable} 才是改表，新建表（无 tableId）不触发。
 */
class FactCollectorHookTest {

    @Test
    void judgeFromArgumentsNotToolName() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        FactStore factStore = new DefaultFactStore(state);
        FactCollectorHook hook = new FactCollectorHook(
                List.of(new TableMutationFactDefinition()), factStore);
        HookEnvironment env = new HookEnvironment("s1", "agent", state);

        // upsertTable 带 tableId → 触发事实采集
        DefaultToolCallContext ctx1 = new DefaultToolCallContext(env, "tc-1", "upsertTable",
                Map.of("tableId", "t-123", "rows", 5));
        HookResult r1 = hook.afterTool(ctx1);
        assertThat(r1).isEqualTo(HookResult.CONTINUE);
        List<Fact> facts = factStore.activeFacts("s1", 1);
        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).producer()).isEqualTo("table-mutation");

        // upsertTable 不带 tableId（新建表）→ 不触发
        DefaultToolCallContext ctx2 = new DefaultToolCallContext(env, "tc-2", "upsertTable",
                Map.of("schema", "new"));
        hook.afterTool(ctx2);
        assertThat(factStore.activeFacts("s1", 1)).hasSize(1); // 仍只有第一条
    }

    @Test
    void factStoredWithTtlFromDefinition() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        FactStore factStore = new DefaultFactStore(state);
        FactCollectorHook hook = new FactCollectorHook(
                List.of(new TableMutationFactDefinition()), factStore); // ttl=3
        HookEnvironment env = new HookEnvironment("s1", "agent", state);

        DefaultToolCallContext ctx = new DefaultToolCallContext(env, "tc-1", "upsertTable",
                Map.of("tableId", "t-1"));
        hook.afterTool(ctx);

        // turn 1-3 active（ttl=3），turn 4 过期
        assertThat(factStore.activeFacts("s1", 1)).hasSize(1);
        assertThat(factStore.activeFacts("s1", 3)).hasSize(1);
        assertThat(factStore.activeFacts("s1", 4)).isEmpty();
    }

    /** 蓝本例 FactDefinition：带 tableId 的 upsertTable 才是改表。 */
    static class TableMutationFactDefinition implements FactDefinition {
        @Override
        public String name() {
            return "table-mutation";
        }

        @Override
        public Optional<Fact> judge(ToolCallContext ctx) {
            // 从入参判定：含 tableId 才是改表现场
            if (!"upsertTable".equals(ctx.toolName())) {
                return Optional.empty();
            }
            Object tableId = ctx.arguments() == null ? null : ctx.arguments().get("tableId");
            if (tableId == null) {
                return Optional.empty(); // 新建表，不触发
            }
            return Optional.of(new Fact("table:" + tableId,
                    Map.of("tableId", tableId), name(), 0, ttl()));
        }

        @Override
        public String render(Fact fact) {
            return "表 " + fact.value() + " 已被修改";
        }

        @Override
        public int ttl() {
            return 3;
        }
    }
}
