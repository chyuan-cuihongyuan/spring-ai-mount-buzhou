package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 危险工具命中分布（spec 833 / T1167，WAF top-rules 观测思想）：危险工具
 * 匹配命中按工具名聚合——「哪个危险工具被碰得最多/涉及哪些要求状态」
 * 从 HITL 提示流水变热力排行（安全画像面）。
 *
 * <p>纯读数：工具封顶 {@value #MAX_TOOLS}（超限并入 overflow 桶——与
 * SkillUsageStats 同款口径）；record(null/空白) 忽略；top(n) 按命中降序。
 * 喂点=DangerousToolGuardHook 命中处装配侧（不改拦截行为）。
 */
public final class DangerousToolHitStats {

    /** 工具数封顶。 */
    public static final int MAX_TOOLS = 64;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单工具命中行。 */
    public record ToolHits(String tool, long hits, String requiredState, long lastSeenMillis) {
    }

    private static final class Counter {
        final AtomicLong hits = new AtomicLong();
        volatile String requiredState = "";
        volatile long lastSeen;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong();

    /** 记录一次命中（null/空白工具名忽略；requiredState 可空）。 */
    public void record(String toolName, String requiredState, long atMillis) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }
        totalHits.incrementAndGet();
        String key = toolName;
        if (!counters.containsKey(key) && counters.size() >= MAX_TOOLS) {
            key = OVERFLOW;
        }
        Counter counter = counters.computeIfAbsent(key, k -> new Counter());
        counter.hits.incrementAndGet();
        if (requiredState != null && !requiredState.isBlank()) {
            counter.requiredState = requiredState;
        }
        counter.lastSeen = Math.max(counter.lastSeen, atMillis);
    }

    /** 命中最多的前 n 个工具（命中降序）。 */
    public List<ToolHits> top(int n) {
        List<ToolHits> all = new ArrayList<>();
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            all.add(new ToolHits(e.getKey(), c.hits.get(), c.requiredState, c.lastSeen));
        }
        all.sort(Comparator.comparingLong(ToolHits::hits).reversed()
                .thenComparing(t -> t.tool()));
        if (n <= 0) {
            return List.of();
        }
        return List.copyOf(all.subList(0, Math.min(n, all.size())));
    }

    /** 累计命中数（含溢出桶）。 */
    public long totalHits() {
        return totalHits.get();
    }

    /** 已登记工具数（含溢出桶）。 */
    public int distinctTools() {
        return counters.size();
    }
}
