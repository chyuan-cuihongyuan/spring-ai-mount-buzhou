package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * spill 回读审计轨迹（spec 539 / T827，60/67 导出族同构）：readRange 每次
 * 回读落一条有界样本（uri/字节数/是否完整性告警/时刻）+ 累计读数。「哪些
 * 证据被反复回读、读了多少、有无完整性告警」的排障读数面。
 *
 * <p>诚实边界：只观测不干预；样本窗有界（128）——重启清零。
 */
public final class ReadAuditTrail {

    /** 默认样本窗容量。 */
    public static final int DEFAULT_WINDOW = 128;

    /** 单次回读样本。 */
    public record ReadRecord(String uri, long bytes, boolean integrityWarning, Instant at) {
    }

    private final int window;
    private final Deque<ReadRecord> records = new ArrayDeque<>();
    private final AtomicLong totalReads = new AtomicLong();
    private final AtomicLong totalBytes = new AtomicLong();
    private final AtomicLong integrityWarnings = new AtomicLong();

    public ReadAuditTrail() {
        this(DEFAULT_WINDOW);
    }

    public ReadAuditTrail(int window) {
        if (window < 1) {
            throw new IllegalArgumentException("窗口容量 >= 1");
        }
        this.window = window;
    }

    /** 记一次回读。 */
    public synchronized void record(String uri, long bytes, boolean integrityWarning, Instant at) {
        records.addLast(new ReadRecord(uri, bytes, integrityWarning, at));
        while (records.size() > window) {
            records.removeFirst();
        }
        totalReads.incrementAndGet();
        totalBytes.addAndGet(Math.max(0, bytes));
        if (integrityWarning) {
            integrityWarnings.incrementAndGet();
        }
    }

    /** 回读样本（旧→新）。 */
    public synchronized List<ReadRecord> recent() {
        return List.copyOf(records);
    }

    /** per-uri 回读计数（窗内样本聚合——降序）。 */
    public synchronized Map<String, Long> uriCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReadRecord r : records) {
            counts.merge(r.uri(), 1L, Long::sum);
        }
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        Map<String, Long> out = new LinkedHashMap<>();
        sorted.forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    public long totalReads() {
        return totalReads.get();
    }

    public long totalBytes() {
        return totalBytes.get();
    }

    public long integrityWarnings() {
        return integrityWarnings.get();
    }
}
