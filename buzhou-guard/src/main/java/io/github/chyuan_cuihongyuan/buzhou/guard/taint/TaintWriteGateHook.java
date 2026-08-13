package io.github.chyuan_cuihongyuan.buzhou.guard.taint;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolMatcher;
import io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint;

import java.time.Instant;
import java.util.Map;

/**
 * FIDES 写门（wayfinder2 impl-21 / T49）：不可信上下文（taint=UNTRUSTED）中的<b>写侧工具
 * 调用</b>被拦截/转 HITL——读写非对称的形式化终点：「tainted 内容未经消毒/审批不得流入
 * 特权动作」。写侧集合 = 既有危险工具清单（不可逆/写类）。审批通道复用既有授权台账
 * （auth.{tool}.{fingerprint}，GuardAuthApi.approve 写回）——即 FIDES「approver」的
 * Buzhou 等价物；人工 approve 后放行（HITL 门在 order 300 做最终裁决）。
 */
public class TaintWriteGateHook implements BuzhouHook {

    public static final String EVENT_TAINT_BLOCKED = "guard.taint.blocked";

    private final DangerousToolConfig config;
    private final DangerousToolMatcher matcher;
    private final SessionStateStore stateStore;

    public TaintWriteGateHook(DangerousToolConfig config, SessionStateStore stateStore) {
        this.config = config;
        this.matcher = new DangerousToolMatcher(config.dangerousTools());
        this.stateStore = stateStore;
    }

    @Override
    public String name() {
        return "TaintWriteGateHook";
    }

    @Override
    public int order() {
        return 250; // Onload(200) 之后、HITL(300) 之前——写门先于最终人工裁决
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (!config.enabled() || matcher.match(ctx.toolName()).isEmpty()) {
            return HookResult.CONTINUE; // 非写侧工具不受 taint 门约束
        }
        if (!TaintTrackingHook.isTainted(stateStore, ctx.sessionId())) {
            return HookResult.CONTINUE; // 可信上下文正常流不受扰
        }
        // 人工已审批同一 (tool, args) → 放行（FIDES approver 等价物；HITL 门做终审）
        String authKey = ArgumentFingerprint.authKey(ctx.toolName(),
                ArgumentFingerprint.fingerprint(ctx.arguments()));
        if (stateStore.get(ctx.sessionId(), authKey).isPresent()) {
            return HookResult.CONTINUE;
        }
        ctx.emitEvent(new SessionEvent(EVENT_TAINT_BLOCKED, Map.of(
                "sessionId", ctx.sessionId(),
                "toolName", ctx.toolName(),
                "reason", "untrusted-context",
                "remedy", "human-approve-or-sanitize"), Instant.now()));
        return new HookResult.Block("等待人工确认（信息流控制）：当前上下文含未消毒的不可信数据"
                + "（来源工具输出，taint=UNTRUSTED），不能直接触发写侧操作「" + ctx.toolName()
                + "」。请用户审批该操作，或先对引用内容消毒后重试。");
    }
}
