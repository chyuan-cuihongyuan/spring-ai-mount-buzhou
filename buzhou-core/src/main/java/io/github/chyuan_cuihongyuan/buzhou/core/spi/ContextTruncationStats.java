package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文截断统计（spec 834 / T1169，HuggingFace tokenizer truncation_strategy
 * 思想——截掉什么/截了多少是模型可见性的侵蚀量）：各截断机制（微压实/
 * spill 外卸/硬裁剪…开集策略名）裁掉的字符量聚合——「模型看不见的上下文
 * 有多少、谁裁的」结构化。
 *
 * <p>纯记账：策略键封顶 {@value #MAX_STRATEGIES}（超限并入 {@link #OVERFLOW}
 * 桶）；record 负值忽略；snapshot 按 chars 降序。喂点=各截断机制装配侧。
 */
public final class ContextTruncationStats {

    /** 策略键封顶。 */
    public static final int MAX_STRATEGIES = 8;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单策略行。 */
    public record StrategyTruncation(String strategy, long events, long charsDropped) {
    }

    /** 不可变报告（chars 降序）。 */
    public record Report(List<StrategyTruncation> strategies, long totalEvents,
                         long totalCharsDropped) {
    }

    private final Map<String, long[]> counters = new LinkedHashMap<>(); // events, chars
    private long totalEvents;
    private long totalChars;

    /** 记录一次截断（null/空白策略或负值字符忽略）。 */
    public synchronized void record(String strategy, long charsDropped) {
        if (strategy == null || strategy.isBlank() || charsDropped < 0) {
            return;
        }
        long[] agg = counters.get(strategy);
        if (agg == null) {
            if (counters.size() >= MAX_STRATEGIES) {
                agg = counters.computeIfAbsent(OVERFLOW, k -> new long[2]);
            } else {
                agg = counters.computeIfAbsent(strategy, k -> new long[2]);
            }
        }
        agg[0]++;
        agg[1] += charsDropped;
        totalEvents++;
        totalChars += charsDropped;
    }

    /** 只读快照（chars 降序）。 */
    public synchronized Report snapshot() {
        List<StrategyTruncation> rows = new ArrayList<>(counters.size());
        for (Map.Entry<String, long[]> e : counters.entrySet()) {
            rows.add(new StrategyTruncation(e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        rows.sort(Comparator.comparingLong(StrategyTruncation::charsDropped).reversed());
        return new Report(List.copyOf(rows), totalEvents, totalChars);
    }
}
