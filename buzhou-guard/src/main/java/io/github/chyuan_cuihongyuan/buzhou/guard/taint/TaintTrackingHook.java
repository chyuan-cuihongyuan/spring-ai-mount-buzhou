package io.github.chyuan_cuihongyuan.buzhou.guard.taint;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.Map;

/**
 * 读侧 taint 打标钩子（wayfinder2 impl-21 / T49 / docs/spec/12 §guard-21，MSRC FIDES 最小落地）：
 * 任何工具输出进入会话即把上下文标签 join 到 <b>UNTRUSTED</b>（保守单调——FIDES
 * 「LLM 响应取输入标签 join」的会话级近似）；标签持久化在会话 state，跨轮/跨实例续接有效。
 *
 * <p>消毒（sanitize）是<b>显式动作</b>：业务确认内容可信后调 {@link #clear}（FIDES 逃生舱的
 * Buzhou 等价物——最小实现不含隔离 LLM/变量隐藏，见 spec 12 Out of Scope 的 FIDES 二期）。
 */
public class TaintTrackingHook implements BuzhouHook {

    /** 上下文 taint 标签的 state 键（值为 UNTRUSTED 时写门生效）。 */
    public static final String STATE_KEY = "taint.context";

    private final SessionStateStore stateStore;

    public TaintTrackingHook(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public String name() {
        return "TaintTrackingHook";
    }

    @Override
    public int order() {
        return 150; // 读侧打标先行（Onload 200 / HITL 300 之前）
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        boolean newlyTainted = !isTainted(stateStore, ctx.sessionId());
        stateStore.put(ctx.sessionId(), new StateEntry(STATE_KEY,
                "UNTRUSTED:" + ctx.toolName(), "TaintTrackingHook", ctx.turn(), null, Instant.now()));
        if (newlyTainted) {
            ctx.emitEvent(new SessionEvent("guard.taint.marked",
                    Map.of("sessionId", ctx.sessionId(), "source", ctx.toolName(),
                            "label", "UNTRUSTED"), Instant.now()));
        }
        return HookResult.CONTINUE;
    }

    /** 当前上下文是否 tainted（无 state = TRUSTED）。 */
    public static boolean isTainted(SessionStateStore stateStore, String sessionId) {
        return stateStore.get(sessionId, STATE_KEY)
                .map(StateEntry::value)
                .filter(value -> value != null && value.startsWith("UNTRUSTED"))
                .isPresent();
    }

    /** 消毒：业务确认上下文可信后显式清除标签（逃生舱；不影响授权台账）。 */
    public static void clear(SessionStateStore stateStore, String sessionId) {
        stateStore.delete(sessionId, STATE_KEY);
    }
}
