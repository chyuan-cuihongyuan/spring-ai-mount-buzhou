package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.guard.taint.TaintTrackingHook;

import java.time.Instant;
import java.util.Map;

/**
 * 策略门钩子（wayfinder2 impl-23 / T52）：beforeTool 经 {@link PolicyEngine} 裁决——
 * allow 放行 / deny 物理阻断（附 reason）/ escalate 转人工确认（等待审批文案，
 * 审批通道复用既有 GuardAuthApi 台账）。label 输入衔接 FIDES taint
 * （{@code taint=UNTRUSTED}）， escalate+已审批 = allow。
 */
public class PolicyGateHook implements BuzhouHook {

    private final PolicyEngine engine;

    public PolicyGateHook(PolicyEngine engine) {
        this.engine = engine;
    }

    @Override
    public String name() {
        return "PolicyGateHook";
    }

    @Override
    public int order() {
        return 275; // taint 写门(250)之后、HITL 门(300)之前——策略作为泛化授权层
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        // label 输入衔接 FIDES taint（state 里 taint.context 值形如 UNTRUSTED:xxx）
        String taint = ctx.state() == null ? null
                : ctx.state().<String>get(TaintTrackingHook.STATE_KEY, String.class).orElse(null);
        PolicyDecision.Input input = new PolicyDecision.Input(
                ctx.agentName(), ctx.toolName(), ctx.arguments(),
                Map.of("taint", taint != null && taint.startsWith("UNTRUSTED")
                        ? "UNTRUSTED" : "TRUSTED"),
                false);
        PolicyDecision decision = engine.decide(input);
        ctx.emitEvent(new SessionEvent("policy.decided", Map.of(
                "sessionId", ctx.sessionId(),
                "toolName", ctx.toolName(),
                "action", decision.action().name(),
                "reason", decision.reason()), Instant.now()));
        return switch (decision.action()) {
            case ALLOW -> HookResult.CONTINUE;
            case DENY -> new HookResult.Block("策略拒绝：" + decision.reason());
            case ESCALATE -> new HookResult.Block("等待人工确认（策略升级）：" + decision.reason()
                    + "；请用户审批后重试。");
        };
    }
}
