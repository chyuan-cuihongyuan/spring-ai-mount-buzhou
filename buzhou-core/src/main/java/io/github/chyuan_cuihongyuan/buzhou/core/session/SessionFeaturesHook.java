package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

/**
 * 特征采集 hook（spec 161 / T519）：beforeTurn（turns++/活跃触达）、afterTool
 * （toolCalls++，结构化错误反馈计 toolErrors++——ToolFeedbackType 标记）、
 * onModelError（modelErrors++）三点自动累积进 {@link SessionFeatureStore}。
 * 挂 hook 即累积，不挂零成本。
 */
public final class SessionFeaturesHook implements BuzhouHook {

    private final SessionFeatureStore store;

    /** spec 1620 / T2391：afterTurn sweep 节拍计数（进程级摊薄）。 */
    private static final java.util.concurrent.atomic.AtomicInteger TURN_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    public SessionFeaturesHook(SessionFeatureStore store) {
        this.store = store == null ? IdleMonitorHolder.store() : store;
    }

    @Override
    public String name() {
        return "SessionFeaturesHook";
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            store.recordTurnStart(ctx.sessionId());
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        // spec 1620 / T2391：空闲监控节拍（每 32 轮 sweep——纯观测旁路；sweep 失败不伤轮次）
        if (TURN_COUNTER.incrementAndGet() % IdleMonitorHolder.SWEEP_EVERY_TURNS == 0) {
            try {
                IdleMonitorHolder.sweepAndRecord(java.time.Instant.now());
            } catch (RuntimeException ignored) {
                // 观测旁路失败静默（下轮再扫）
            }
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            store.recordToolCall(ctx.sessionId(),
                    ctx.result() instanceof String text
                            && io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolFeedbackType
                                    .isErrorFeedback(text));
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult onModelError(ModelCallContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            store.recordModelError(ctx.sessionId());
        }
        return HookResult.CONTINUE;
    }

    /** 特征仓直读（查询/测试）。 */
    public SessionFeatureStore store() {
        return store;
    }
}
