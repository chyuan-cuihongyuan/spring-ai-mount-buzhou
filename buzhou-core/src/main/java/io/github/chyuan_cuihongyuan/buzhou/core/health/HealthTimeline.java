package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康状态变迁时间线（spec 405 / T701，PagerDuty incident timeline 借鉴）：
 * 有界环——快照 diff 仅变迁入环（无变迁零记录，环不被无信息行占满）；
 * 首次现身记 from=null（初见语义）。容量 = 内存上界。
 */
public final class HealthTimeline {

    /** 一次变迁（from=null = 首次现身）。 */
    public record Entry(String mechanism, BuzhouHealth.Status from, BuzhouHealth.Status to, Instant at) {
    }

    /** 默认容量。 */
    public static final int DEFAULT_CAPACITY = 256;

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final Map<String, BuzhouHealth.Status> lastSeen = new LinkedHashMap<>();

    public HealthTimeline() {
        this(DEFAULT_CAPACITY);
    }

    public HealthTimeline(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0: " + capacity);
        }
        this.capacity = capacity;
    }

    /** 记一轮快照：与上一轮 diff，仅变迁入环；返回本轮新增项。 */
    public synchronized List<Entry> record(Map<String, BuzhouHealth.Status> snapshot, Instant at) {
        List<Entry> fresh = new ArrayList<>();
        for (Map.Entry<String, BuzhouHealth.Status> e : snapshot.entrySet()) {
            BuzhouHealth.Status from = lastSeen.get(e.getKey());
            if (from == e.getValue()) {
                continue; // 无变迁
            }
            Entry entry = new Entry(e.getKey(), from, e.getValue(), at);
            lastSeen.put(e.getKey(), e.getValue());
            entries.addLast(entry);
            fresh.add(entry);
            while (entries.size() > capacity) {
                entries.pollFirst();
            }
        }
        return List.copyOf(fresh);
    }

    /** 全部变迁（旧→新快照副本）。 */
    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** per-mechanism 变迁计数（抖动识别面——高频 UP↔DOWN 一眼可辨）。 */
    public synchronized Map<String, Long> transitionCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Entry e : entries) {
            counts.merge(e.mechanism(), 1L, Long::sum);
        }
        return counts;
    }

    /** 容量。 */
    public int capacity() {
        return capacity;
    }
}
