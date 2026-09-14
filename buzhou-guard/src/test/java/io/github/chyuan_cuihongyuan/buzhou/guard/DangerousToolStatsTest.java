package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook;
import java.util.List;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1069 / impl 821：危险工具守卫判定读面——禁用/未匹配/已授权/豁免/
 * 升级确认五结局桶守恒恒等式、resetForTest 归零。
 * 装配骨架经 GuardModule（危险工具 yml 配置）驱动真实 matcher。
 */
class DangerousToolStatsTest {

    private HookEnvironment env;

    @BeforeEach
    void setUp() {
        DangerousToolGuardHook.resetForTest();
        env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    private DangerousToolGuardHook hook() {
        // 危险工具清单经 GuardModule 装配（yml 条目集），此处直取装配产物
        var module = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        var guardModule = GuardModule.fromYml(module, Map.of(
                "dangerous-tools", List.of(
                        Map.of("name", "write_file", "hint", "写入需确认"))));
        return guardModule.configure().hooks().stream()
                .filter(DangerousToolGuardHook.class::isInstance)
                .map(DangerousToolGuardHook.class::cast)
                .findFirst().orElseThrow();
    }

    private HookResult invoke(DangerousToolGuardHook hook, String tool) {
        return hook.beforeTool(new DefaultToolCallContext(env, "tc", tool, Map.of()));
    }

    @Test
    void unmatchedToolCountsItsBucket() {
        DangerousToolGuardHook hook = hook();
        assertThat(invoke(hook, "read_file")).isEqualTo(HookResult.CONTINUE);

        DangerousToolGuardHook.DangerousToolStats stats = DangerousToolGuardHook.stats();
        assertThat(stats.invocations()).isEqualTo(1);
        assertThat(stats.unmatchedSkips()).isEqualTo(1);
        assertThat(stats.escalations()).isZero();
    }

    @Test
    void dangerousToolEscalates() {
        DangerousToolGuardHook hook = hook();
        HookResult result = invoke(hook, "write_file");
        assertThat(result).isNotEqualTo(HookResult.CONTINUE); // 升级确认（等待人工）

        DangerousToolGuardHook.DangerousToolStats stats = DangerousToolGuardHook.stats();
        assertThat(stats.escalations()).isEqualTo(1);
        assertThat(stats.unmatchedSkips()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        DangerousToolGuardHook hook = hook();
        invoke(hook, "read_file");    // unmatched
        invoke(hook, "write_file");   // escalations

        DangerousToolGuardHook.DangerousToolStats stats = DangerousToolGuardHook.stats();
        assertThat(stats.invocations()).isEqualTo(2);
        assertThat(stats.invocations())
                .isEqualTo(stats.disabledSkips() + stats.unmatchedSkips()
                        + stats.authorizedSkips() + stats.exemptedSkips()
                        + stats.escalations());
    }

    @Test
    void resetForTestZeroesCounters() {
        DangerousToolGuardHook hook = hook();
        invoke(hook, "read_file");
        assertThat(DangerousToolGuardHook.stats().invocations()).isEqualTo(1);

        DangerousToolGuardHook.resetForTest();

        DangerousToolGuardHook.DangerousToolStats stats = DangerousToolGuardHook.stats();
        assertThat(stats.invocations()).isZero();
        assertThat(stats.escalations()).isZero();
    }
}
