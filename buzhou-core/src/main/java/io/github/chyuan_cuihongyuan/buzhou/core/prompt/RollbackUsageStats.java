package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt 回滚使用读数（spec 845 / T1191，S6 备选池——Langfuse rollback 借鉴
 * 既有回滚机制的观测面）：按 prompt 名聚合回滚事件（回滚次数/最近回滚时刻/
 * 最近目标版本）——「哪些 prompt 在反复回滚」（不稳定提示的温度计）。
 *
 * <p>纯记账：prompt 名开集封顶 {@value #MAX_PROMPTS}（超限并入 overflow 桶
 * ——跨域口径一致）；record(null/空白/负版本) 忽略；snapshot 次数降序。
 * 喂点=回滚执行处装配侧。
 */
public final class RollbackUsageStats {

    /** prompt 名封顶。 */
    public static final int MAX_PROMPTS = 64;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单 prompt 行。 */
    public record PromptRollbacks(String name, long rollbacks, int lastFromVersion,
                                  int lastToVersion, long lastSeenMillis) {
    }

    /** 不可变报告（次数降序）。 */
    public record Report(List<PromptRollbacks> prompts, long total) {
    }

    private static final class Counter {
        long rollbacks;
        int lastFrom = -1;
        int lastTo = -1;
        long lastSeen;
    }

    private final Map<String, Counter> counters = new LinkedHashMap<>();
    private long total;

    /** 记录一次回滚（版本号须 ≥0；null/空白名忽略）。 */
    public synchronized void record(String name, int fromVersion, int toVersion, long atMillis) {
        if (name == null || name.isBlank() || fromVersion < 0 || toVersion < 0) {
            return;
        }
        Counter counter = counters.get(name);
        if (counter == null) {
            if (counters.size() >= MAX_PROMPTS) {
                counter = counters.computeIfAbsent(OVERFLOW, k -> new Counter());
            } else {
                counter = counters.computeIfAbsent(name, k -> new Counter());
            }
        }
        counter.rollbacks++;
        counter.lastFrom = fromVersion;
        counter.lastTo = toVersion;
        counter.lastSeen = Math.max(counter.lastSeen, atMillis);
        total++;
    }

    /** 只读快照（次数降序，键名典序破平）。 */
    public Report snapshot() {
        List<PromptRollbacks> rows = new ArrayList<>(counters.size());
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            rows.add(new PromptRollbacks(e.getKey(), c.rollbacks, c.lastFrom, c.lastTo, c.lastSeen));
        }
        rows.sort(Comparator.comparingLong(PromptRollbacks::rollbacks).reversed()
                .thenComparing(PromptRollbacks::name));
        return new Report(List.copyOf(rows), total);
    }
}
