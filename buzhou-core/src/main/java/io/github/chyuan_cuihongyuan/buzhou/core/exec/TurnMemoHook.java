package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

/**
 * 轮清零 hook（spec 147 / T501）：beforeTurn（order 30）清 memo 表——
 * 每轮白纸。持与 {@link MemoizedToolCallback} 同一 {@link TurnMemo} 引用。
 */
public final class TurnMemoHook implements BuzhouHook {

    public static final int ORDER = 30;

    private final TurnMemo memo;

    public TurnMemoHook(TurnMemo memo) {
        this.memo = memo == null ? new TurnMemo() : memo;
    }

    @Override
    public String name() {
        return "TurnMemoHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        memo.clear();
        return HookResult.CONTINUE;
    }

    /** memo 直读（观测/测试）。 */
    public TurnMemo memo() {
        return memo;
    }
}
