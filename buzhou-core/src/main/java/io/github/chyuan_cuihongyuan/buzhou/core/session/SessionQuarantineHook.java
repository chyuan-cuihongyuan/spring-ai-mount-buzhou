package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

/**
 * 会话检疫 hook（spec 143 / T496）：beforeTurn（order 40——最早裁决）准入
 * （隔离中 block 可读理由，不抛——模型与用户拿到「检疫中剩 Xs」）；
 * onModelError 计一次失败。成功复位走 {@link SessionQuarantine#recordTurnSuccess}
 * 公共 API（hook 面看不到「健康轮」全貌——不谎装自动复位，诚实边界）。
 */
public final class SessionQuarantineHook implements BuzhouHook {

    public static final int ORDER = 40;

    private final SessionQuarantine quarantine;

    public SessionQuarantineHook(SessionQuarantine quarantine) {
        this.quarantine = quarantine == null ? new SessionQuarantine() : quarantine;
    }

    @Override
    public String name() {
        return "SessionQuarantineHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        try {
            quarantine.admitOrThrow(ctx.sessionId());
            return HookResult.CONTINUE;
        } catch (io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException e) {
            return HookResult.block(e.getMessage());
        }
    }

    @Override
    public HookResult onModelError(ModelCallContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            quarantine.recordTurnFailure(ctx.sessionId());
        }
        return HookResult.CONTINUE;
    }

    /** 检疫器直读（观测/测试）。 */
    public SessionQuarantine quarantine() {
        return quarantine;
    }
}
