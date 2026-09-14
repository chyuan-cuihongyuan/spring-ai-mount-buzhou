package io.github.chyuan_cuihongyuan.buzhou.guard.leak;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 跨会话泄漏金丝雀 hook（spec 1625 / T2401，spec 528 孤类接线）：
 * beforeTurn 种植（或复取）会话专属令牌（确定性 sha256——同会话恒同）；
 * afterModel 扫描模型输出——出现<b>其他会话</b>的令牌即跨会话污染信号，
 * 发 {@code guard.session.leak-detected} 事件（from/token）。
 *
 * <p><b>诚实边界（spec 528 原注）</b>：检测依赖令牌原样出现（模型改写/截断
 * 不保——概率探针非隔离机制）；令牌进入会话数据的注入面归宿主（本 hook 只
 * 登记 + 扫描——thinkst canarytokens 的 honeytoken 需放进数据才可被触发）。
 */
public final class SessionCanaryHook implements BuzhouHook {

    /** 泄漏事件类型。 */
    public static final String EVENT_LEAK_DETECTED = "guard.session.leak-detected";

    private final SessionCanaryRegistry registry;

    public SessionCanaryHook(SessionCanaryRegistry registry) {
        this.registry = registry == null
                ? new SessionCanaryRegistry("buzhou-default-leak-canary-salt") : registry;
    }

    /** 注册表直读（观测/宿主取令牌注入数据）。 */
    public SessionCanaryRegistry registry() {
        return registry;
    }

    @Override
    public String name() {
        return "SessionCanaryHook";
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            registry.plant(ctx.sessionId());
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        if (ctx == null || ctx.sessionId() == null || ctx.response() == null
                || ctx.response().chatResponse() == null || ctx.response().chatResponse().getResult() == null
                || ctx.response().chatResponse().getResult().getOutput() == null) {
            return HookResult.CONTINUE;
        }
        String output = ctx.response().chatResponse().getResult().getOutput().getText();
        List<SessionCanaryRegistry.LeakFrom> leaks = registry.detect(ctx.sessionId(), output);
        for (SessionCanaryRegistry.LeakFrom leak : leaks) {
            ctx.emitEvent(new SessionEvent(EVENT_LEAK_DETECTED, Map.of(
                    "sessionId", ctx.sessionId(),
                    "leakedFromSession", leak.leakedFromSession(),
                    "token", leak.token()), Instant.now()));
        }
        return HookResult.CONTINUE;
    }
}
