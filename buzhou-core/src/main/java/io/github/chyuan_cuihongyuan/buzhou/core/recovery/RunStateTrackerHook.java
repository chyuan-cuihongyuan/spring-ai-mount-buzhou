package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

import java.util.function.Supplier;

/**
 * Run 状态追踪钩子（wayfinder2 impl-06 / T32）：turn 开始/完结时向 {@link RunRegistry}
 * 持久化快照——以 Completed-Turn 为快照单元，崩溃后枚举 RUNNING run 即得续跑点
 * （lastCompletedTurn 之后）。会话正常关闭时置 COMPLETED（经观察者，见 {@link RecoverySupport}）。
 */
public class RunStateTrackerHook implements BuzhouHook {

    private final RunRegistry registry;
    private final Supplier<String> appId;
    private final Supplier<String> ownerId;

    public RunStateTrackerHook(RunRegistry registry, String appId, String ownerId) {
        this(registry, () -> appId, () -> ownerId);
    }

    public RunStateTrackerHook(RunRegistry registry, Supplier<String> appId,
                               Supplier<String> ownerId) {
        this.registry = registry;
        this.appId = appId;
        this.ownerId = ownerId;
    }

    @Override
    public String name() {
        return "run-state-tracker";
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        registry.save(current(ctx).startingTurn(ctx.turn(), ownerId.get()));
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        registry.save(current(ctx).completingTurn(ctx.turn(), ownerId.get()));
        return HookResult.CONTINUE;
    }

    private RunStateSnapshot current(TurnContext ctx) {
        return registry.find(ctx.sessionId()).orElseGet(() -> new RunStateSnapshot(
                ctx.sessionId(), appId.get(), ctx.agentName(), RunStatus.RUNNING,
                ctx.turn(), 0, ownerId.get(), null));
    }
}
