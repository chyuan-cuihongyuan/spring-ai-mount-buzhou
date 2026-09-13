package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 半开探测成功率读数（spec 836 / T1173，Resilience4j permitted probe 语义
 * 扩散）：per-model 半开探测成败计数+近窗成功率+连续失败 streak——「半开
 * 是在恢复还是反复被打回 OPEN」量化（与 811 crash-loop 互补：那是跳闸频次、
 * 这是探测质量）。
 *
 * <p>纯读数：模型封顶 {@value #MAX_MODELS}（超限 truncated 不记）；近窗环
 * {@value #WINDOW}（boolean 环）；record(null/空白) 忽略。喂点=探测结果处
 * 装配侧。
 */
public final class HalfOpenProbeStats {

    /** 模型数封顶。 */
    public static final int MAX_MODELS = 32;
    /** 近窗容量。 */
    public static final int WINDOW = 20;

    /** 不可变读数。 */
    public record ProbeStats(String model, long successes, long failures, int consecutiveFailures,
                             double recentSuccessRate) {
    }

    private static final class State {
        final Deque<Boolean> window = new ArrayDeque<>(WINDOW);
        long successes;
        long failures;
        int consecutiveFailures;
    }

    private final Map<String, State> models = new ConcurrentHashMap<>();
    private volatile boolean truncated;

    /** 记录一次探测结果（success=true 成功；null/空白模型忽略）。 */
    public void record(String model, boolean success) {
        if (model == null || model.isBlank()) {
            return;
        }
        State state = models.get(model);
        if (state == null) {
            if (models.size() >= MAX_MODELS) {
                truncated = true;
                return;
            }
            state = models.computeIfAbsent(model, k -> new State());
        }
        synchronized (state) {
            if (success) {
                state.successes++;
                state.consecutiveFailures = 0;
            } else {
                state.failures++;
                state.consecutiveFailures++;
            }
            if (state.window.size() >= WINDOW) {
                state.window.pollFirst();
            }
            state.window.addLast(success);
        }
    }

    /** 单模型读数（未知 null；近窗成功率为环内均值）。 */
    public ProbeStats stats(String model) {
        State state = model == null ? null : models.get(model);
        if (state == null) {
            return null;
        }
        synchronized (state) {
            long recentSuccesses = 0;
            for (Boolean ok : state.window) {
                if (ok) {
                    recentSuccesses++;
                }
            }
            double rate = state.window.isEmpty() ? 0 : (double) recentSuccesses / state.window.size();
            return new ProbeStats(model, state.successes, state.failures,
                    state.consecutiveFailures, rate);
        }
    }

    public boolean truncated() {
        return truncated;
    }
}
