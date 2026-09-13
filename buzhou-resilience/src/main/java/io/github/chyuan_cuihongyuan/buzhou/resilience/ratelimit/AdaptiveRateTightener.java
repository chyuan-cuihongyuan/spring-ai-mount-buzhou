package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流自适应收紧器（spec 818 / T1137，AWS SDK adaptive mode 客户端节流借鉴）：
 * 供应商 429（throttling 错误）后按系数收紧本地放行乘数——「已知上游在挤，
 * 就少发一点」避免重试风暴加剧拥塞；安静期过后按步长逐步恢复到 1.0。
 *
 * <p><b>纯时间计算</b>（无后台线程）：{@link #effectiveMultiplier(String, long)}
 * 由 now 相对 lastThrottle 的位置推导收紧保持/步进恢复；同 now 恒同值
 * （确定性）。恢复是乘性步进（AIMD 的 MI 侧）；收紧是乘性收缩（MD 侧）。
 * 模型封顶 {@value #MAX_MODELS}（truncated 如实）。
 */
public final class AdaptiveRateTightener {

    /** 模型数封顶。 */
    public static final int MAX_MODELS = 32;

    private final double minMultiplier;
    private final double shrinkFactor;
    private final long tightHoldMillis;
    private final long recoverStepMillis;
    private final double recoverFactor;
    private final Map<String, ModelState> models = new ConcurrentHashMap<>();
    private volatile boolean truncated;

    private static final class ModelState {
        volatile double multiplier = 1.0;
        volatile long lastThrottleMillis = Long.MIN_VALUE;
    }

    /**
     * @param minMultiplier     收紧下限（0,1]；乘数不再低于此值
     * @param shrinkFactor      每次收紧乘的系数（0,1）
     * @param tightHoldMillis   收紧后保持时长（此后开始步进恢复）
     * @param recoverStepMillis 每步恢复间隔
     * @param recoverFactor     每步恢复乘的系数（&gt;1，封顶 1.0）
     */
    public AdaptiveRateTightener(double minMultiplier, double shrinkFactor,
                                 long tightHoldMillis, long recoverStepMillis, double recoverFactor) {
        if (minMultiplier <= 0 || minMultiplier > 1) {
            throw new IllegalArgumentException("minMultiplier ∈ (0,1]（当前 " + minMultiplier + "）");
        }
        if (shrinkFactor <= 0 || shrinkFactor >= 1) {
            throw new IllegalArgumentException("shrinkFactor ∈ (0,1)（当前 " + shrinkFactor + "）");
        }
        if (tightHoldMillis < 0 || recoverStepMillis < 1) {
            throw new IllegalArgumentException("tightHoldMillis >= 0 且 recoverStepMillis >= 1");
        }
        if (recoverFactor <= 1) {
            throw new IllegalArgumentException("recoverFactor > 1（当前 " + recoverFactor + "）");
        }
        this.minMultiplier = minMultiplier;
        this.shrinkFactor = shrinkFactor;
        this.tightHoldMillis = tightHoldMillis;
        this.recoverStepMillis = recoverStepMillis;
        this.recoverFactor = recoverFactor;
    }

    /** 记录一次 429（收紧乘数+重置保持窗；未知模型建态）。 */
    public void onThrottled(String model, long atMillis) {
        ModelState state = stateOf(model);
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.multiplier = Math.max(minMultiplier, state.multiplier * shrinkFactor);
            state.lastThrottleMillis = atMillis;
        }
    }

    /**
     * 当前放行乘数 ∈ [minMultiplier, 1]：收紧保持期内返回收紧值；之后每
     * recoverStepMillis 恢复一步（×recoverFactor）向 1.0 渐进；未收紧=1。
     * 纯时间推导（同参同值，无副作用）。
     */
    public double effectiveMultiplier(String model, long nowMillis) {
        ModelState state = model == null ? null : models.get(model);
        if (state == null) {
            return 1.0;
        }
        synchronized (state) {
            double tightened = state.multiplier;
            if (tightened >= 1.0 || state.lastThrottleMillis == Long.MIN_VALUE) {
                return tightened;
            }
            long sinceThrottle = nowMillis - state.lastThrottleMillis;
            if (sinceThrottle <= tightHoldMillis) {
                return tightened;
            }
            long over = sinceThrottle - tightHoldMillis;
            int steps = (int) (over / recoverStepMillis);
            double recovered = tightened * Math.pow(recoverFactor, steps);
            return Math.min(1.0, recovered);
        }
    }

    /** 模型当前是否处于收紧（乘数 <1）态。 */
    public boolean isTightened(String model, long nowMillis) {
        return effectiveMultiplier(model, nowMillis) < 1.0;
    }

    public boolean truncated() {
        return truncated;
    }

    private ModelState stateOf(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        ModelState state = models.get(model);
        if (state == null) {
            if (models.size() >= MAX_MODELS) {
                truncated = true;
                return null;
            }
            state = models.computeIfAbsent(model, k -> new ModelState());
        }
        return state;
    }
}
