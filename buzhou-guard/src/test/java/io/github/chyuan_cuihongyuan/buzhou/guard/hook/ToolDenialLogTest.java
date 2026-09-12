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
 * 角色权限拒绝有界日志测试（spec 709 / T969–T970 / impl 512）：拒绝双记、
 * reason 分流、环形封顶、聚合 64 键封顶、无 log 构造零回归、快照不可变。
 */
class ToolDenialLogTest {

    private static ToolPermissions sample() {
        return new ToolPermissions(Map.of(
                "viewer", List.of("read_file"),
                "editor", List.of("read_file", "write_file")));
    }

    private static HookEnvironment envWithRole(String role) {
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        env.stateHandle().put(ToolRoleGuardHook.ROLE_STATE_KEY, role);
        return env;
    }

    @Test
    void denialsRecordedWithReasonSplit() {
        ToolDenialLog log = new ToolDenialLog();
        ToolRoleGuardHook hook = new ToolRoleGuardHook(sample(), "default", log);

        // 未授权：有角色无权限
        hook.beforeTool(new DefaultToolCallContext(envWithRole("viewer"), "t1", "write_file", Map.of()));
        // fail-closed：未定义角色
        hook.beforeTool(new DefaultToolCallContext(envWithRole("ghost"), "t2", "run_command", Map.of()));

        assertThat(log.entries()).hasSize(2);
        assertThat(log.entries().get(0).reason()).isEqualTo(ToolDenialLog.Reason.UNDEFINED_ROLE);
        assertThat(log.entries().get(0).role()).isEqualTo("ghost");
        assertThat(log.entries().get(1).reason()).isEqualTo(ToolDenialLog.Reason.UNAUTHORIZED);
        assertThat(log.entries().get(1).toolName()).isEqualTo("write_file");
        assertThat(log.topDenials()).containsEntry("ghost->run_command", 1L)
                .containsEntry("viewer->write_file", 1L);
    }

    @Test
    void ringCapsOldestEvicted() {
        ToolDenialLog log = new ToolDenialLog();
        for (int i = 0; i < ToolDenialLog.RING_CAPACITY + 1; i++) {
            log.record("r" + i, "tool", ToolDenialLog.Reason.UNAUTHORIZED, 1000L + i);
        }
        assertThat(log.entries()).hasSize(ToolDenialLog.RING_CAPACITY);
        assertThat(log.entries().get(0).role()).isEqualTo("r" + ToolDenialLog.RING_CAPACITY); // 最新在前
        assertThat(log.entries().getLast().role()).isEqualTo("r1"); // r0 最老被挤
    }

    @Test
    void aggregateCapsAt64AndFlagsTruncated() {
        ToolDenialLog log = new ToolDenialLog();
        for (int i = 0; i < ToolDenialLog.AGGREGATE_CAP + 10; i++) {
            log.record("role-" + i, "tool", ToolDenialLog.Reason.UNAUTHORIZED, i);
        }
        assertThat(log.truncated()).isTrue();
        assertThat(log.topDenials()).containsKey("_truncated");
        assertThat(log.topDenials()).doesNotContainKey("role-70->tool"); // 超限键未计入
        assertThat(log.topDenials()).containsKey("role-63->tool");
        assertThat(log.entries()).hasSize(ToolDenialLog.AGGREGATE_CAP + 10); // 明细环独立计数
    }

    @Test
    void snapshotsAreImmutable() {
        ToolDenialLog log = new ToolDenialLog();
        log.record("r", "t", ToolDenialLog.Reason.UNAUTHORIZED, 1L);
        assertThat(log.entries()).isUnmodifiable();
        assertThat(log.topDenials()).isUnmodifiable();
    }

    @Test
    void noLogConstructorKeepsLegacyBehavior() {
        ToolRoleGuardHook hook = new ToolRoleGuardHook(sample());
        HookResult verdict = hook.beforeTool(
                new DefaultToolCallContext(envWithRole("viewer"), "t1", "write_file", Map.of()));
        assertThat(verdict).isNotEqualTo(HookResult.CONTINUE); // 仍拒绝
        // 无 log 实例可查——既有行为（计数）不受影响
    }
}
