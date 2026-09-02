package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolFeedbackType;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;

/**
 * 错误预算喂数 hook（spec 321 / T633）：order 250（熔断 240 后、HITL 300 前）
 * <b>纯观察</b>——beforeTool 恒 CONTINUE（不拦不拒），afterTool 按结构化标记记
 * 结局（{@link ToolFeedbackType#isErrorFeedback}——与熔断 hook 131 同语义：
 * 执行失败与校验失败均计败）。挂进 RuntimeConfig 即喂数，不挂零变化。
 */
public final class ErrorBudgetHook implements BuzhouHook {

    public static final int ORDER = 250;

    private final ErrorBudget budget;

    public ErrorBudgetHook(ErrorBudget budget) {
        this.budget = budget == null
                ? new ErrorBudget(new ErrorBudget.Config(99,
                        ErrorBudget.Config.DEFAULT_BURN_THRESHOLD,
                        ErrorBudget.Config.DEFAULT_BUCKETS,
                        ErrorBudget.Config.DEFAULT_WINDOW,
                        ErrorBudget.Config.DEFAULT_MIN_SAMPLES),
                        java.time.Clock.systemDefaultZone())
                : budget;
    }

    @Override
    public String name() {
        return "ErrorBudgetHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        return HookResult.CONTINUE; // 纯观察——预算只计不拦
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null) {
            return HookResult.CONTINUE;
        }
        Object result = ctx.result();
        boolean failure = result instanceof String text && ToolFeedbackType.isErrorFeedback(text);
        budget.record(ctx.toolName(), !failure);
        return HookResult.CONTINUE;
    }

    /** 预算直读（观测/测试）。 */
    public ErrorBudget budget() {
        return budget;
    }
}
