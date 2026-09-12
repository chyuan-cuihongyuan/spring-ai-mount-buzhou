package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * hook 计时健康面（spec 647 / T944–T945）：恒 UP（观测辅助面——hook 慢≠机制
 * 失能）；details = 进程级 per-hook 计时（count / totalMicros / maxMicros /
 * avgMicros，{@link HookTimingAggregator} 聚合同源）。挂
 * {@code /actuator/buzhou} 快照的 {@code hook-timing} 段——「哪个 hook 吃掉
 * Turn 内联预算」一屏可读。
 */
public final class HookTimingHealth implements BuzhouHealth {

    /** details 有界纪律：hook 名封顶（超限截断——正常 hook 数远低于此）。 */
    static final int TOP_LIMIT = 20;

    private final HookTimingAggregator aggregator;

    public HookTimingHealth(HookTimingAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    public String mechanism() {
        return "hook-timing";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, HookChain.HookTiming> stats = aggregator.stats();
        Map<String, Long> windowed = aggregator.windowedMax(); // spec 708：滚动窗 max
        Map<String, Object> out = new LinkedHashMap<>();
        int i = 0;
        for (Map.Entry<String, HookChain.HookTiming> e : stats.entrySet()) {
            if (i++ >= TOP_LIMIT) {
                out.put("_truncated", true);
                break;
            }
            HookChain.HookTiming t = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("count", t.count());
            row.put("totalMicros", t.totalNanos() / 1_000);
            row.put("maxMicros", t.maxNanos() / 1_000);
            row.put("avgMicros", (long) t.avgNanos() / 1_000);
            row.put("rollingMaxMicros", windowed.getOrDefault(e.getKey(), 0L) / 1_000);
            out.put(e.getKey(), row);
        }
        return out;
    }
}
