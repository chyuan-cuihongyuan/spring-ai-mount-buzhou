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

    public SessionFeaturesHook(SessionFeatureStore store) {
        this.store = store == null ? new SessionFeatureStore() : store;
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
