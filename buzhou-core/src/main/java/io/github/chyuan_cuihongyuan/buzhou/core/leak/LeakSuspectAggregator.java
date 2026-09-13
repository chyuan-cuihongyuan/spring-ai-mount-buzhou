package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 泄漏疑似对象聚合器（spec 839 / T1179，资源泄漏检测聚合思想——S9 备选池；
 * {@link ResourceLeakDetector} LeakListener 的聚合实现）：逐条泄漏报告按
 * 描述键（截 {@value #KEY_MAX} 字符稳键）聚合计数/最大龄/最近龄——「泄漏
 * 是同一处反复漏还是多处散漏」从日志流水变排行。
 *
 * <p>纯聚合：键封顶 {@value #MAX_KEYS}（超限并入 overflow 桶）；实现
 * {@code LeakListener} 可直接挂 {@code addLeakListener}（若提供）或调用方
 * 转发 onLeak——检测器行为零变更。null 报告忽略。
 */
public final class LeakSuspectAggregator implements ResourceLeakDetector.LeakListener {

    /** 键数封顶。 */
    public static final int MAX_KEYS = 32;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";
    /** 稳定键截断长度。 */
    public static final int KEY_MAX = 64;

    /** 单类疑似行。 */
    public record SuspectType(String key, long count, long maxAgeMillis, long lastSeenMillis) {
    }

    /** 不可变报告（count 降序）。 */
    public record Report(List<SuspectType> suspects, long totalLeaks, boolean truncated) {
    }

    private final Map<String, long[]> counters = new LinkedHashMap<>(); // count, maxAge, lastSeen
    private long totalLeaks;
    private volatile boolean truncated;

    @Override
    public void onLeak(ResourceLeakDetector.LeakReport report) {
        if (report == null || report.description() == null || report.description().isBlank()) {
            return;
        }
        String raw = report.description();
        String key = raw.length() <= KEY_MAX ? raw : raw.substring(0, KEY_MAX);
        long age = Math.max(0, report.ageMillis());
        long[] agg = counters.get(key);
        if (agg == null) {
            if (counters.size() >= MAX_KEYS) {
                truncated = true;
                agg = counters.computeIfAbsent(OVERFLOW, k -> new long[3]);
            } else {
                agg = counters.computeIfAbsent(key, k -> new long[3]);
            }
        }
        synchronized (agg) {
            agg[0]++;
            agg[1] = Math.max(agg[1], age);
            agg[2] = System.currentTimeMillis();
        }
        totalLeaks++;
    }

    /** 只读快照（count 降序，键名典序破平）。 */
    public Report snapshot() {
        List<SuspectType> rows = new ArrayList<>(counters.size());
        for (Map.Entry<String, long[]> e : counters.entrySet()) {
            synchronized (e.getValue()) {
                rows.add(new SuspectType(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
            }
        }
        rows.sort(Comparator.comparingLong(SuspectType::count).reversed()
                .thenComparing(SuspectType::key));
        return new Report(List.copyOf(rows), totalLeaks, truncated);
    }
}
