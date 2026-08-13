package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

/**
 * sleep-time 触发钩子（wayfinder2 impl-11 / T37）：turn 完结后按频率投递
 * {@link SleepTimeConsolidator} 整理任务——热路径零阻塞（submit 即返回）。
 */
public class SleepTimeConsolidationHook implements BuzhouHook {

    private final SleepTimeScheduler scheduler;
    private final SleepTimeConsolidator consolidator;
    private final int everyTurns;

    public SleepTimeConsolidationHook(SleepTimeScheduler scheduler,
                                      SleepTimeConsolidator consolidator,
                                      int everyTurns) {
        this.scheduler = scheduler;
        this.consolidator = consolidator;
        this.everyTurns = Math.max(1, everyTurns);
    }

    @Override
    public String name() {
        return "sleep-time-consolidation";
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        if (ctx.turn() % everyTurns == 0) {
            scheduler.submit(ctx.sessionId(), () -> consolidator.consolidate(ctx.sessionId()));
        }
        return HookResult.CONTINUE;
    }
}
