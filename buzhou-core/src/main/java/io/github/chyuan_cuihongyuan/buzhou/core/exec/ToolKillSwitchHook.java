package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 工具紧急停用 hook（spec 325 / T641，LaunchDarkly kill switch / K8s
 * cordon 借鉴）：order 15（维护门 10 后、模型闸 20 前）——停用集内工具
 * block「[工具已停用]」前缀（<b>非错误标记</b>：人为停用不是失败，blocked
 * 不走 afterTool，熔断/错误预算零污染）。事故按钮运行时
 * {@link #disableTools}/{@link #enableTools}/{@link #clearAll} 不重启即生效；
 * 监听 {@code BuzhouConfigRefreshEvent}（320 通道）整体重读 yml 覆盖停用集
 * ——yml 是事实源。空集纯直通（装配恒在：事故按钮必须预先存在才有用）。
 */
public final class ToolKillSwitchHook implements BuzhouHook {

    public static final int ORDER = 15;
    static final String MARKER = "[工具已停用]";

    private final CopyOnWriteArraySet<String> disabled = new CopyOnWriteArraySet<>();

    @Override
    public String name() {
        return "ToolKillSwitchHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null || disabled.isEmpty()) {
            return HookResult.CONTINUE;
        }
        if (!disabled.contains(ctx.toolName())) {
            return HookResult.CONTINUE;
        }
        return HookResult.block(MARKER + "\n工具：" + ctx.toolName()
                + "\n原因：紧急停用（事故响应）——平台值班已全局停用该工具；"
                + "请改用其他工具或等待恢复公告");
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE; // 停用只拦不篡改
    }

    /** 事故按钮：停用一批工具（即时生效）。 */
    public void disableTools(Set<String> tools) {
        if (tools != null) {
            disabled.addAll(tools);
        }
    }

    /** 恢复一批工具。 */
    public void enableTools(Set<String> tools) {
        if (tools != null) {
            disabled.removeAll(tools);
        }
    }

    /** 全部恢复。 */
    public void clearAll() {
        disabled.clear();
    }

    /** 当前停用集（只读视图）。 */
    public Set<String> disabled() {
        return Set.copyOf(disabled);
    }

    /** 刷新事件整体覆盖（yml 是事实源——运行时应急改动被收编）。 */
    void replaceDisabled(Set<String> tools) {
        disabled.clear();
        if (tools != null) {
            disabled.addAll(new HashSet<>(tools));
        }
    }
}
