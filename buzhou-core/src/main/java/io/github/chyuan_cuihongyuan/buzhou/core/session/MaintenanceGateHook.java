package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

/**
 * 维护模式门 hook（spec 205 / T577）：beforeTurn（order 10——维护面最高
 * 优先级）维护中 block 温和文案 + 计数；维护关即透传（零变化）。
 */
public final class MaintenanceGateHook implements BuzhouHook {

    public static final int ORDER = 10;
    static final String BLOCKED_COUNTER = "buzhou.maintenance.blocked";

    private final MaintenanceGate gate;

    public MaintenanceGateHook(MaintenanceGate gate) {
        this.gate = gate == null ? new MaintenanceGate() : gate;
    }

    @Override
    public String name() {
        return "MaintenanceGateHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx == null || !gate.isActive()) {
            return HookResult.CONTINUE;
        }
        BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, 1);
        return HookResult.block(gate.refusalMessage());
    }

    /** 门直读（观测/测试）。 */
    public MaintenanceGate gate() {
        return gate;
    }
}
