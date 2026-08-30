package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ToolPermissions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 141 / T492：角色工具权限回归——通配三形 / 会话态角色 / 未设走 default /
 * 未定义角色 fail-closed / block 文案。
 */
class ToolRoleGuardHookTest {

    private static ToolPermissions sample() {
        return new ToolPermissions(Map.of(
                "viewer", List.of("read_file", "log*"),
                "editor", List.of("read_file", "write_file"),
                "admin", List.of(ToolPermissions.ANY)));
    }

    private HookEnvironment env() {
        return new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
    }

    @Test
    void wildcardFormsMatchAsDeclared() {
        ToolPermissions permissions = sample();
        assertThat(permissions.allows("viewer", "read_file")).isTrue();   // 精确
        assertThat(permissions.allows("viewer", "log_search")).isTrue(); // 前缀
        assertThat(permissions.allows("viewer", "login_check")).isTrue(); // log 也是 login 的前缀——前缀语义如此（诚实面）
        assertThat(permissions.allows("viewer", "catalog_query")).isFalse(); // 前缀在开头才匹配
        assertThat(permissions.allows("viewer", "write_file")).isFalse();
        assertThat(permissions.allows("editor", "write_file")).isTrue();
        assertThat(permissions.allows("admin", "run_command")).isTrue();  // 全放
    }

    @Test
    void roleReadFromSessionStateAndBlockedWhenDenied() {
        ToolRoleGuardHook hook = new ToolRoleGuardHook(sample());
        HookEnvironment env = env();
        env.stateHandle().put(ToolRoleGuardHook.ROLE_STATE_KEY, "viewer");

        DefaultToolCallContext denied =
                new DefaultToolCallContext(env, "tc1", "write_file", Map.of());
        HookResult verdict = hook.beforeTool(denied);
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason())
                .contains("viewer").contains("write_file");

        DefaultToolCallContext allowed =
                new DefaultToolCallContext(env, "tc2", "log_search", Map.of());
        assertThat(hook.beforeTool(allowed)).isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void unsetRoleFallsBackToDefaultRole() {
        ToolPermissions permissions = new ToolPermissions(Map.of(
                "default", List.of("read_file"),
                "admin", List.of(ToolPermissions.ANY)));
        ToolRoleGuardHook hook = new ToolRoleGuardHook(permissions);
        HookEnvironment env = env(); // 未写角色键

        assertThat(hook.beforeTool(
                new DefaultToolCallContext(env, "tc1", "read_file", Map.of())))
                .isEqualTo(HookResult.CONTINUE);
        HookResult denied = hook.beforeTool(
                new DefaultToolCallContext(env, "tc2", "run_command", Map.of()));
        assertThat(denied).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) denied).reason()).contains("default");
    }

    @Test
    void undefinedRoleFailsClosed() {
        ToolRoleGuardHook hook = new ToolRoleGuardHook(sample());
        HookEnvironment env = env();
        env.stateHandle().put(ToolRoleGuardHook.ROLE_STATE_KEY, "adminn"); // 拼错

        HookResult verdict = hook.beforeTool(
                new DefaultToolCallContext(env, "tc1", "read_file", Map.of()));
        assertThat(verdict).isInstanceOf(HookResult.Block.class);
        assertThat(((HookResult.Block) verdict).reason())
                .contains("未在权限规则中定义").contains("fail-closed");
    }

    @Test
    void roleChangeTakesEffectImmediatelyWithoutCaching() {
        ToolRoleGuardHook hook = new ToolRoleGuardHook(sample());
        HookEnvironment env = env();

        env.stateHandle().put(ToolRoleGuardHook.ROLE_STATE_KEY, "viewer");
        assertThat(hook.beforeTool(
                new DefaultToolCallContext(env, "tc1", "write_file", Map.of())))
                .isInstanceOf(HookResult.Block.class);

        // 会话中途升级 admin——即时放行（无缓存）
        env.stateHandle().put(ToolRoleGuardHook.ROLE_STATE_KEY, "admin");
        assertThat(hook.beforeTool(
                new DefaultToolCallContext(env, "tc2", "write_file", Map.of())))
                .isEqualTo(HookResult.CONTINUE);
    }

    @Test
    void emptyPatternListDeniesAllForRole() {
        ToolPermissions permissions = new ToolPermissions(Map.of(
                "nobody", List.of()));
        assertThat(permissions.allows("nobody", "read_file")).isFalse();
        assertThat(permissions.hasRole("nobody")).isTrue();
    }
}
