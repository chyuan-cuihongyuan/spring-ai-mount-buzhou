package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重试主机排除（spec 2008 / T3117 / impl 1559）——Envoy retry host
 * predicate 思想：重试不落同一坏端点——冷却窗内失败过的 host 从候选
 * 中滤除（attempt-1 失败者让位），冷却到期自动回归；候选全被排除时
 * 回退全量（可用性优先——排除是偏好不是硬门，不制造空候选空转）。
 *
 * <p>synchronized 小临界区；时间由调用方传入（确定性可回放）。
 */
public final class RetryHostExclusion {

    /** 默认冷却窗（毫秒）——失败后 30s 内重试绕行。 */
    public static final long DEFAULT_COOLDOWN_MILLIS = 30_000L;

    private final long cooldownMillis;
    private final Map<String, Long> lastFailureAt = new LinkedHashMap<>();

    /** 契约：cooldownMillis &gt; 0（fail-fast）。 */
    public RetryHostExclusion(long cooldownMillis) {
        if (cooldownMillis <= 0) {
            throw new IllegalArgumentException("cooldownMillis 须 > 0：" + cooldownMillis);
        }
        this.cooldownMillis = cooldownMillis;
    }

    public RetryHostExclusion() {
        this(DEFAULT_COOLDOWN_MILLIS);
    }

    /** 记一次候选失败（nowMillis 单调由调用方保证）。 */
    public synchronized void recordFailure(String host, long nowMillis) {
        if (host == null) {
            throw new IllegalArgumentException("host 不能为 null");
        }
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        lastFailureAt.put(host, nowMillis);
    }

    /**
     * 过滤候选：剔除冷却窗内失败过的 host；全被剔除时返回原序全量
     * （排除全排除回退——Envoy predicate 只降优先级不置空的同款语义）。
     */
    public synchronized List<String> filterCandidates(List<String> candidates, long nowMillis) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates 不能为 null");
        }
        List<String> filtered = candidates.stream()
                .filter(h -> !isCooling(h, nowMillis))
                .toList();
        return filtered.isEmpty() ? List.copyOf(candidates) : filtered;
    }

    /** 冷却中（将被排除）的候选数——非空断言对账面。 */
    public synchronized int excludedCount(List<String> candidates, long nowMillis) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates 不能为 null");
        }
        return (int) candidates.stream().filter(h -> isCooling(h, nowMillis)).count();
    }

    private boolean isCooling(String host, long nowMillis) {
        Long failedAt = lastFailureAt.get(host);
        return failedAt != null && nowMillis - failedAt < cooldownMillis;
    }
}
