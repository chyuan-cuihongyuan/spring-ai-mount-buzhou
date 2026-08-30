package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolFeedbackType;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

/**
 * 工具熔断 hook（spec 131 / T478）：beforeTool（order 240，先于 HITL 300）——
 * OPEN/半开超额的工具直接拒（{@code HookResult.block} 带冷却提示，模型可读可改道）；
 * afterTool 按结构化标记记结局（{@link ToolFeedbackType#isErrorFeedback}——执行失败
 * 与校验失败均计败）。挂进 RuntimeConfig 即启用，不挂零变化。
 */
public final class ToolCircuitBreakerHook implements BuzhouHook {

    public static final int ORDER = 240;
    static final String BLOCKED_COUNTER = "buzhou.tool-breaker.blocked";

    private final ToolCircuitBreaker breaker;

    public ToolCircuitBreakerHook(ToolCircuitBreaker breaker) {
        this.breaker = breaker == null ? new ToolCircuitBreaker() : breaker;
    }

    @Override
    public String name() {
        return "ToolCircuitBreakerHook";
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
        ToolCircuitBreaker.View view = breaker.stateOf(ctx.toolName());
        if (!breaker.tryAcquirePermission(ctx.toolName())) {
            BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, "tool", ctx.toolName());
            long waitSec = Math.max(1, (view.cooldownRemainingMillis() + 999) / 1000);
            String hint = view.state() == ToolCircuitBreaker.State.HALF_OPEN
                    ? "工具熔断半开探测中（探测名额已满，稍后重试）"
                    : "工具熔断中（约 " + waitSec + "s 后进入半开探测；请改用其他工具或稍后重试）";
            return HookResult.block("工具「" + ctx.toolName() + "」暂时不可用：" + hint);
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null) {
            return HookResult.CONTINUE;
        }
        Object result = ctx.result();
        if (result instanceof String text && ToolFeedbackType.isErrorFeedback(text)) {
            breaker.recordFailure(ctx.toolName());
        } else {
            breaker.recordSuccess(ctx.toolName());
        }
        return HookResult.CONTINUE;
    }

    /** 熔断器直读（观测/测试）。 */
    public ToolCircuitBreaker breaker() {
        return breaker;
    }
}
