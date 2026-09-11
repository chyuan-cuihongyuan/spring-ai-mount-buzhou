package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时延 SLO 燃尽监视（spec 509 / T769，Google SRE workbook——321 错误预算
 * 的时延维度扩散）：坏事件 = 轮端到端时长 > thresholdMillis；计时法与
 * {@code TurnTimingHook} 同法（beforeTurn 起点 LRU 1024/重入覆盖/异常缺
 * afterTurn 不计样本），燃尽语义全量复用 {@link ErrorBudget}
 * （burnRate/breaching/topBreaching/min-samples 防噪声——不造第二状态机）。
 *
 * <p>诚实边界：端到端口径（模型+工具全链）；只观测不拦截；enabled 默认关。
 */
public final class LatencySloMonitor implements BuzhouHook {

    private static final int MAX_SESSIONS = 1024;

    private final long thresholdMillis;
    private final ErrorBudget budget;
    private final Map<String, Long> startNanos = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    public LatencySloMonitor(long thresholdMillis, ErrorBudget budget) {
        if (thresholdMillis <= 0) {
            throw new IllegalArgumentException("threshold-millis > 0（当前 " + thresholdMillis + "）");
        }
        if (budget == null) {
            throw new IllegalArgumentException("ErrorBudget 必须非空");
        }
        this.thresholdMillis = thresholdMillis;
        this.budget = budget;
    }

    @Override
    public String name() {
        return "LatencySloMonitor";
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        synchronized (startNanos) {
            startNanos.put(ctx.sessionId(), System.nanoTime());
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        Long start;
        synchronized (startNanos) {
            start = startNanos.remove(ctx.sessionId());
        }
        if (start == null) {
            return HookResult.CONTINUE; // 无起点——异常路径，不计样本
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        budget.record(ctx.agentName(), elapsedMillis <= thresholdMillis);
        return HookResult.CONTINUE;
    }

    /** 底层燃尽面（burnRate/breaching/topBreaching——321 观测语义全量可用）。 */
    public ErrorBudget budget() {
        return budget;
    }

    public long thresholdMillis() {
        return thresholdMillis;
    }
}
