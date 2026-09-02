package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 干跑拦截 hook（spec 323 / T637，Terraform plan/apply 借鉴）：order 290
 * （HITL 300 前）——干跑开时 beforeTool 拦下入计划（block「[干跑拦截]」
 * 前缀——<b>非错误标记</b>：干跑不是失败，blocked 不走 afterTool，熔断/
 * 错误预算不被演练污染），模型可读可改道。计划面 {@link #plan()} 有界 100
 * 条 + dropped 计数（诚实截断）；include 清单空 = 全量拦（纯演练）；
 * 运行时 {@link #setEnabled} 窗口启停。
 */
public final class DryRunHook implements BuzhouHook {

    public static final int ORDER = 290;
    static final String MARKER = "[干跑拦截]";
    static final int MAX_PLANNED = 100;

    /** 一条将执行而未执行的调用（args 快照——后续变更不影响已记计划）。 */
    public record PlannedCall(String toolCallId, String toolName, Map<String, Object> arguments) {
    }

    private final Set<String> tools;
    private volatile boolean enabled;
    private final List<PlannedCall> planned = new ArrayList<>();
    private long dropped;

    /**
     * @param tools   include 清单（空 = 全量拦——纯演练语义）
     * @param enabled 初始开关（装配 true；测试可关）
     */
    public DryRunHook(Set<String> tools, boolean enabled) {
        this.tools = tools == null ? Set.of() : Set.copyOf(tools);
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "DryRunHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (!enabled || ctx == null || ctx.toolName() == null) {
            return HookResult.CONTINUE;
        }
        if (!tools.isEmpty() && !tools.contains(ctx.toolName())) {
            return HookResult.CONTINUE; // 清单外照常真跑
        }
        synchronized (planned) {
            if (planned.size() < MAX_PLANNED) {
                planned.add(new PlannedCall(ctx.toolCallId(), ctx.toolName(),
                        ctx.arguments() == null ? Map.of() : Map.copyOf(ctx.arguments())));
            } else {
                dropped++;
            }
        }
        return HookResult.block(MARKER + "\n工具：" + ctx.toolName()
                + "\n原因：干跑模式——本次调用已记入执行计划未执行（计划面 dryRunPlan 可审）");
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE; // 干跑只拦执行不篡改结果
    }

    /** 执行计划快照（有界；顺序即拦截序）。 */
    public List<PlannedCall> plan() {
        synchronized (planned) {
            return List.copyOf(planned);
        }
    }

    /** 超界丢弃的计划条数（诚实截断观测面）。 */
    public long droppedCount() {
        synchronized (planned) {
            return dropped;
        }
    }

    /** 清计划（新演练窗口）。 */
    public void clearPlan() {
        synchronized (planned) {
            planned.clear();
            dropped = 0;
        }
    }

    /** 运行时启停（干跑窗口——不重启）。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
