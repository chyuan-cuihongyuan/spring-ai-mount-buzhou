package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行耗时健康面（spec 700 / T951–T952）：恒 UP（观测辅助面——工具慢≠
 * 机制失能）；details = 进程级 per-tool 耗时聚合（count / totalMicros /
 * maxMicros / avgMicros / failed，{@link ToolTimingAggregator} 聚合同源）。挂
 * {@code /actuator/buzhou} 快照的 {@code tool-timing} 段——「哪个工具吃掉
 * 工具耗时预算」一屏可读。
 */
public final class ToolTimingHealth implements BuzhouHealth {

    /** details 有界纪律：工具名封顶（超限截断——正常工具数远低于此）。 */
    static final int TOP_LIMIT = 20;

    private final ToolTimingAggregator aggregator;

    public ToolTimingHealth(ToolTimingAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    public String mechanism() {
        return "tool-timing";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        // pg_stat_statements 语义：按总耗时降序取 top——确定性读面 + 截断裁掉的恰是最不重要行
        List<ToolTimingAggregator.ToolTiming> sorted = new ArrayList<>(aggregator.stats().values());
        sorted.sort(Comparator.comparingLong(ToolTimingAggregator.ToolTiming::totalNanos)
                .reversed()
                .thenComparing(ToolTimingAggregator.ToolTiming::toolName));
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (i >= TOP_LIMIT) {
                out.put("_truncated", true);
                break;
            }
            ToolTimingAggregator.ToolTiming t = sorted.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("count", t.count());
            row.put("totalMicros", t.totalNanos() / 1_000);
            row.put("maxMicros", t.maxNanos() / 1_000);
            row.put("avgMicros", t.avgNanos() / 1_000);
            row.put("failed", t.failed());
            out.put(t.toolName(), row);
        }
        return out;
    }
}
