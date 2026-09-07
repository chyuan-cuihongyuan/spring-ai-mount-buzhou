package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 舱压伸缩建议（spec 319 / T629，Kubernetes HPA custom metrics 借鉴——
 * desired = current × metric/target 的阈值比整数简化）：跨调用窗口读
 * 舱拒绝<b>增量</b> → 建议实例倍率 clamp(1 + 窗口拒绝/scaleUpThreshold,
 * 1, maxMultiplier)。窗口 = 两次 {@link #advise()} 调用之间（无内部定时——
 * 节奏归宿主）；拒绝回零建议回落 1（HPA minReplicas 语义，不为 0）。
 *
 * <p>只建议不扩容：执行归宿主/调度器（诚实边界）。有拒绝历史的 agent 才进
 * 建议面（从未被拒 = 隐式 1，无需建议）；256 封顶折叠行（{@code __overflow__}）
 * 按普通名透传。输入缝 = {@link AgentBulkhead#topRejections} 既有观测面
 * （per-agent 计数单调，快照稳定可比）。
 */
public final class BulkheadScalingAdvisor {

    /** 单 agent 建议快照（不可变——观测面/调度器输入）。 */
    public record Advice(String agent, long windowRejections, int suggestedMultiplier) {
    }

    private final AgentBulkhead bulkhead;
    private final long scaleUpThreshold;
    private final int maxMultiplier;
    private final Map<String, Long> lastRejections = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastMultipliers = new ConcurrentHashMap<>();
    private final AtomicLong scaleUpAdvice = new AtomicLong();
    private final AtomicLong scaleDownAdvice = new AtomicLong();
    private volatile Map<String, Advice> lastAdvice = Map.of();

    /**
     * @param bulkhead         舱（拒绝计数源）
     * @param scaleUpThreshold 每多少个窗口拒绝 = +1 倍实例（≥1）
     * @param maxMultiplier    建议倍率上限（≥1；回落下限恒 1）
     */
    public BulkheadScalingAdvisor(AgentBulkhead bulkhead, long scaleUpThreshold,
            int maxMultiplier) {
        if (bulkhead == null) {
            throw new IllegalArgumentException("bulkhead 必须非空");
        }
        if (scaleUpThreshold < 1) {
            throw new IllegalArgumentException("scaleUpThreshold >= 1（当前 " + scaleUpThreshold + "）");
        }
        if (maxMultiplier < 1) {
            throw new IllegalArgumentException("maxMultiplier >= 1（当前 " + maxMultiplier + "）");
        }
        this.bulkhead = bulkhead;
        this.scaleUpThreshold = scaleUpThreshold;
        this.maxMultiplier = maxMultiplier;
    }

    /**
     * 结一个窗口出全部建议（拒绝数降序、同数字典序——输出稳定）；窗口增量 =
     * 本次快照 − 上次（计数单调，不会为负）。倍率 &gt;1 计 scale-up 事件、
     * 从 &gt;1 回落 1 计 scale-down 事件（metrics + 类内观测计数）。
     */
    public synchronized Map<String, Advice> advise() {
        Map<String, Long> current = new HashMap<>();
        for (Map.Entry<String, Long> entry : bulkhead.topRejections(Integer.MAX_VALUE)) {
            current.put(entry.getKey(), entry.getValue());
        }
        Map<String, Advice> advice = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            String agent = entry.getKey();
            long window = entry.getValue() - lastRejections.getOrDefault(agent, 0L);
            int multiplier = (int) Math.min(maxMultiplier,
                    Math.max(1L, 1 + window / scaleUpThreshold));
            advice.put(agent, new Advice(agent, window, multiplier));
            Integer previous = lastMultipliers.get(agent);
            if (multiplier > 1) {
                scaleUpAdvice.incrementAndGet();
                BuzhouMetricsHolder.metrics().counter("buzhou.bulkhead.scaling.scale-up");
            } else if (previous != null && previous > 1) {
                scaleDownAdvice.incrementAndGet();
                BuzhouMetricsHolder.metrics().counter("buzhou.bulkhead.scaling.scale-down");
            }
            lastMultipliers.put(agent, multiplier);
        }
        lastRejections.clear();
        lastRejections.putAll(current);
        lastAdvice = Map.copyOf(advice);
        return lastAdvice;
    }

    /** 最近一次建议（不结新窗口——观测面）。 */
    public Map<String, Advice> lastAdvice() {
        return lastAdvice;
    }

    /** scale-up 建议事件累计（观测面）。 */
    public long scaleUpAdviceCount() {
        return scaleUpAdvice.get();
    }

    /** scale-down（回落）建议事件累计（观测面）。 */
    public long scaleDownAdviceCount() {
        return scaleDownAdvice.get();
    }

    /** 每多少窗口拒绝 = +1 倍（观测面）。 */
    public long scaleUpThreshold() {
        return scaleUpThreshold;
    }

    /** 建议倍率上限（观测面）。 */
    public int maxMultiplier() {
        return maxMultiplier;
    }
}
