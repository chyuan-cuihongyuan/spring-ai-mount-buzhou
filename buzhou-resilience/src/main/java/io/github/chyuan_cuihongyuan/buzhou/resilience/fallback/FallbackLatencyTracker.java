package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 备模型延迟 EMA 追踪（spec 64 §A / T279 / effort#24，LiteLLM latency-based routing
 * 思想）：每次备模型/金丝雀调用记录耗时（毫秒），指数移动平均（α={@value #ALPHA}，
 * O(1) 无窗内存）反映近期表现——慢化趋势在数次调用内传导到排序。
 *
 * <p><b>排序语义</b>：已知 EMA 升序 + 原序稳定并列；<b>未知延迟取已知 EMA 的中位数</b>
 * （中性：新上链模型不插队也不饿死；全未知 = 原序不动）。线程安全（ConcurrentHashMap）。
 *
 * @since 1.0.0
 */
public final class FallbackLatencyTracker {

    /** EMA 平滑系数（0.3：约 3-4 个样本内主导）。 */
    public static final double ALPHA = 0.3;

    private final ConcurrentHashMap<String, Double> emaMs = new ConcurrentHashMap<>();

    /** 记录一次调用耗时（毫秒；非正值忽略）。 */
    public void record(String modelName, long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        emaMs.compute(modelName, (k, prev) -> prev == null
                ? (double) durationMs
                : ALPHA * durationMs + (1 - ALPHA) * prev);
    }

    /** 当前 EMA（无数据 = null）。 */
    public Double emaMs(String modelName) {
        return emaMs.get(modelName);
    }

    /**
     * 延迟感知排序视图（不修改入参）：已知 EMA 升序、未知取中位数、并列保原序。
     */
    public List<NamedFallbackModel> sorted(List<NamedFallbackModel> candidates) {
        if (candidates == null || candidates.size() <= 1 || emaMs.isEmpty()) {
            return candidates;
        }
        double median = medianOfKnown(candidates);
        List<NamedFallbackModel> copy = new ArrayList<>(candidates);
        copy.sort(Comparator.comparingDouble(
                (NamedFallbackModel m) -> emaMs.getOrDefault(m.name(), median)));
        return copy; // List.sort 稳定：并列（含同取中位数的未知项）保原序
    }

    private double medianOfKnown(List<NamedFallbackModel> candidates) {
        List<Double> known = new ArrayList<>();
        for (NamedFallbackModel m : candidates) {
            Double e = emaMs.get(m.name());
            if (e != null) {
                known.add(e);
            }
        }
        if (known.isEmpty()) {
            return Double.MAX_VALUE; // 全未知：排序无效果（比较值相同 → 原序）
        }
        known.sort(Double::compare);
        int mid = known.size() / 2;
        return known.size() % 2 == 1 ? known.get(mid)
                : (known.get(mid - 1) + known.get(mid)) / 2.0;
    }
}
