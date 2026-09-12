package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ToolPermissions;

/**
 * 角色工具权限 hook（spec 141 / T491）：beforeTool（order 260——熔断 240 之后、
 * HITL 300 之前）读会话态 {@value #ROLE_STATE_KEY} 得角色（未设 = default），
 * 未授权 block（角色名+工具名+修法）；未定义角色 fail-closed 全拒。
 *
 * <p>不缓存角色（每调用读态——会话中途升降级即时生效）。与
 * {@link DangerousToolGuardHook} 正交：角色管面、HITL 管次。
 */
public final class ToolRoleGuardHook implements BuzhouHook {

    public static final int ORDER = 260;
    /** 会话态角色键（宿主在会话建立/登录态写入）。 */
    public static final String ROLE_STATE_KEY = "buzhou.tool-role";
    static final String DENIED_COUNTER = "buzhou.tool-role.denied";

    private final ToolPermissions permissions;
    private final String defaultRole;
    private final ToolDenialLog denialLog;

    public ToolRoleGuardHook(ToolPermissions permissions) {
        this(permissions, "default");
    }

    public ToolRoleGuardHook(ToolPermissions permissions, String defaultRole) {
        this(permissions, defaultRole, null);
    }

    /** spec 709 / T969：+denialLog（null = 不记日志——默认零行为变化）。 */
    public ToolRoleGuardHook(ToolPermissions permissions, String defaultRole,
                             ToolDenialLog denialLog) {
        this.permissions = permissions == null ? new ToolPermissions(null) : permissions;
        this.defaultRole = defaultRole == null || defaultRole.isBlank()
                ? "default" : defaultRole;
        this.denialLog = denialLog;
    }

    @Override
    public String name() {
        return "ToolRoleGuardHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null) {
            return HookResult.CONTINUE;
        }
        String role = ctx.state().get(ROLE_STATE_KEY, String.class).orElse(defaultRole);
        if (permissions.allows(role, ctx.toolName())) {
            return HookResult.CONTINUE;
        }
        BuzhouMetricsHolder.metrics().counter(DENIED_COUNTER, 1, "role", role);
        // spec 709 / T969：拒绝双记（明细 + 聚合；log 未装配跳过）
        if (denialLog != null) {
            denialLog.record(role, ctx.toolName(),
                    permissions.hasRole(role)
                            ? ToolDenialLog.Reason.UNAUTHORIZED
                            : ToolDenialLog.Reason.UNDEFINED_ROLE,
                    System.currentTimeMillis());
        }
        String hint = permissions.hasRole(role)
                ? "角色「" + role + "」无权调用工具「" + ctx.toolName() + "」（可用工具面见角色配置；如需临时扩面请提升角色）"
                : "角色「" + role + "」未在权限规则中定义（fail-closed 全拒；请修正会话角色或补角色规则）";
        return HookResult.block(hint);
    }
}
