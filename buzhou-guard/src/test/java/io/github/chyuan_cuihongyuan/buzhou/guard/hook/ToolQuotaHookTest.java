package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 185 / T558：per-tool 会话配额回归——超限拦 / 通配优先级 / 会话隔离 /
 * 被拒不计 / 未列名零限制。
 */
class ToolQuotaHookTest {

    private HookEnvironment env() {
        return new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    private HookResult invoke(ToolQuotaHook hook, HookEnvironment env, String tool) {
        return hook.beforeTool(new DefaultToolCallContext(env, "tc", tool, Map.of()));
    }

    @Test
    void reachesLimitAndBlocksWhileBelowPasses() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("expensive", 2));
        HookEnvironment env = env();

        assertThat(invoke(hook, env, "expensive")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, env, "expensive")).isEqualTo(HookResult.CONTINUE);
        HookResult verdict = invoke(hook, env, "expensive");
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason())
                .contains("expensive").contains("2 次");

        // 被拒不计——连续拒不放大计数（仍是 2）
        assertThat(invoke(hook, env, "expensive")).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void namedLimitTakesPriorityOverWildcard() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("special", 1, "*", 5));
        HookEnvironment env = env();

        assertThat(invoke(hook, env, "special")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, env, "special")).isInstanceOf(HookResult.Block.class); // 列名 1 次
        assertThat(invoke(hook, env, "other")).isEqualTo(HookResult.CONTINUE); // 通配 5 次
    }

    @Test
    void sessionsAreIsolatedByStateLifecycle() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("t", 1));
        HookEnvironment one = env();
        HookEnvironment two = env();

        assertThat(invoke(hook, one, "t")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, one, "t")).isInstanceOf(HookResult.Block.class);
        assertThat(invoke(hook, two, "t")).isEqualTo(HookResult.CONTINUE); // 别的会话独立窗
    }

    @Test
    void toolsAreIndependentWithinSession() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("a", 1, "b", 1));
        HookEnvironment env = env();
        assertThat(invoke(hook, env, "a")).isEqualTo(HookResult.CONTINUE);
        assertThat(invoke(hook, env, "b")).isEqualTo(HookResult.CONTINUE); // 互不挤占
        assertThat(invoke(hook, env, "a")).isInstanceOf(HookResult.Block.class);
    }

    @Test
    void unlistedWithoutWildcardMeansNoLimit() {
        ToolQuotaHook hook = new ToolQuotaHook(Map.of("only-this", 1));
        HookEnvironment env = env();
        for (int i = 0; i < 10; i++) {
            assertThat(invoke(hook, env, "free-tool")).isEqualTo(HookResult.CONTINUE);
        }
    }

    @Test
    void nullContextSafe() {
        assertThat(new ToolQuotaHook(Map.of()).beforeTool(null)).isEqualTo(HookResult.CONTINUE);
    }
}
