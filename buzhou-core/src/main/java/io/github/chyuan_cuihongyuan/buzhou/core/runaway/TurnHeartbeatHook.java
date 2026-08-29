package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

import java.time.Instant;

/**
 * 轮次心跳钩子（spec 152 §A / T505，spec 138 的接线面）：Turn 生命周期对齐
 * 在飞表（beforeTurn 注册 / afterTurn 清除），模型与工具的before/after 四点
 * 自动打点——宿主不再手工调 {@link TurnHeartbeat}。
 *
 * <p><b>order {@value #ORDER}</b>：early 段（runaway 1000 / budget 1100 之前）——
 * 后续钩子 block/替换也先留痕「到过这里」，停滞检测的进展信号更完整。
 * 永不干预：全 {@link HookResult#CONTINUE}（观测面只打点不裁决——与观测钩子
 * 同纪律）。时钟统一 {@code Instant.now()} 外注点收敛在 beat 调用处（测试替身
 * 走 TurnHeartbeat 单测，本钩子不引入第二时钟面）。
 */
public final class TurnHeartbeatHook implements BuzhouHook {

    /** 早段序（裁决类钩子之前——先留痕后裁决）。 */
    public static final int ORDER = 50;

    private final TurnHeartbeat heartbeat;

    public TurnHeartbeatHook(TurnHeartbeat heartbeat) {
        this.heartbeat = heartbeat == null ? new TurnHeartbeat() : heartbeat;
    }

    /** 共享同一个心跳表的视图（宿主巡检 stalled 用）。 */
    public TurnHeartbeat heartbeat() {
        return heartbeat;
    }

    @Override
    public String name() {
        return "TurnHeartbeatHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        heartbeat.register(ctx.sessionId(), Instant.now());
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        heartbeat.clear(ctx.sessionId());
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        heartbeat.beat(ctx.sessionId(), Instant.now());
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        heartbeat.beat(ctx.sessionId(), Instant.now());
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        heartbeat.beat(ctx.sessionId(), Instant.now());
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        heartbeat.beat(ctx.sessionId(), Instant.now());
        return HookResult.CONTINUE;
    }
}
