package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轮内模型调用循环闸（spec 197 / T569）：beforeModel（order 20——最早裁决）
 * 按 (session, turn) 计数，超上限 block（可读理由——费用风暴最后总闸）；
 * afterTurn 主动清键；LRU 1024。只数模型调用（工具循环归 runaway 语义面）。
 */
public final class ModelCallCapHook implements BuzhouHook {

    public static final int ORDER = 20;
    static final String BLOCKED_COUNTER = "buzhou.model-cap.blocked";
    private static final int MAX_SESSIONS = 1024;

    private final int maxCallsPerTurn;

    private final LinkedHashMap<String, Integer> counters =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                    return size() > MAX_SESSIONS;
                }
            };

    public ModelCallCapHook() {
        this(32);
    }

    public ModelCallCapHook(int maxCallsPerTurn) {
        if (maxCallsPerTurn < 1) {
            throw new IllegalArgumentException("maxCallsPerTurn>=1（当前 " + maxCallsPerTurn + "）");
        }
        this.maxCallsPerTurn = maxCallsPerTurn;
    }

    @Override
    public String name() {
        return "ModelCallCapHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        String key = ctx.sessionId() + ":" + ctx.turn();
        int count;
        synchronized (counters) {
            count = counters.merge(key, 1, Integer::sum);
        }
        if (count > maxCallsPerTurn) {
            BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, 1);
            return HookResult.block("本轮模型调用已达上限（" + maxCallsPerTurn
                    + " 次，当前第 " + count + " 次）——疑似调用环路，请检查重试/REASK 配置");
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        if (ctx != null && ctx.sessionId() != null) {
            synchronized (counters) {
                counters.remove(ctx.sessionId() + ":" + ctx.turn());
            }
        }
        return HookResult.CONTINUE;
    }

    /** 当前 (session, turn) 计数（观测/测试）。 */
    public int currentCount(String sessionId, int turn) {
        synchronized (counters) {
            return counters.getOrDefault(sessionId + ":" + turn, 0);
        }
    }
}
