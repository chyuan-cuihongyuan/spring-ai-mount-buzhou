package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * impl-758 / spec 1005：工具在飞并发水位读面（Go runtime {@code NumGoroutine}
 * 水位 / Hystrix 并发执行观测借鉴——current/peak 双水位）。
 *
 * <p>与限流面（spec 05 信号量、泳道、舱）正交：限流回答「允许多少」，本读面
 * 回答「实际多少、峰值到过多少、哪个工具卡住（在飞长期不归零）」。
 *
 * <p>per-tool 计数以工具目录规模为界（{@link ToolTimingAggregator} per-tool
 * map 同先例，不违无界纪律）。接线点：{@code HookedToolCallback} try/finally。
 */
public final class ToolInFlight {

    private ToolInFlight() {
    }

    /** 单工具水位（不可变快照行）。 */
    public record PerTool(int current, long peak, long total) {
    }

    /** 进程级在飞快照（不可变）。 */
    public record Snapshot(long currentTotal, long peakTotal, Map<String, PerTool> perTool) {

        public Snapshot {
            perTool = Map.copyOf(perTool);
        }
    }

    /** 在飞租约（close 恰一次——重复 close 无害）。 */
    public static final class Lease implements AutoCloseable {

        private final String toolName;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(String toolName) {
            this.toolName = toolName;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                leave(toolName);
            }
        }
    }

    private static final class Counter {
        private final AtomicInteger current = new AtomicInteger();
        private final LongAdder total = new LongAdder();
        private final AtomicLong peak = new AtomicLong();
    }

    private static final ConcurrentHashMap<String, Counter> PER_TOOL = new ConcurrentHashMap<>();
    private static final AtomicInteger CURRENT_TOTAL = new AtomicInteger();
    private static final AtomicLong PEAK_TOTAL = new AtomicLong();

    /**
     * 开一个在飞租约：current +1（工具级与全局），total +1，峰值 CAS 只增不降。
     * 必须配对 {@code close()}（try/finally 惯用法）。
     */
    public static Lease enter(String toolName) {
        Counter counter = PER_TOOL.computeIfAbsent(
                toolName == null ? "" : toolName, k -> new Counter());
        counter.current.incrementAndGet();
        counter.total.increment();
        counter.peak.updateAndGet(prev -> Math.max(prev, counter.current.get()));
        int totalNow = CURRENT_TOTAL.incrementAndGet();
        PEAK_TOTAL.updateAndGet(prev -> Math.max(prev, totalNow));
        return new Lease(toolName);
    }

    private static void leave(String toolName) {
        Counter counter = PER_TOOL.get(toolName == null ? "" : toolName);
        if (counter == null) {
            return;
        }
        counter.current.decrementAndGet();
        CURRENT_TOTAL.decrementAndGet();
    }

    /** 在飞水位只读快照（不可变）。 */
    public static Snapshot snapshot() {
        Map<String, PerTool> out = new HashMap<>();
        PER_TOOL.forEach((name, counter) -> out.put(name,
                new PerTool(counter.current.get(), counter.peak.get(), counter.total.sum())));
        return new Snapshot(CURRENT_TOTAL.get(), PEAK_TOTAL.get(), out);
    }

    /**
     * 清零全部水位（测试隔离注入点——BuzhouMetricsHolder 进程态先例；生产勿调）。
     */
    public static void reset() {
        PER_TOOL.clear();
        CURRENT_TOTAL.set(0);
        PEAK_TOTAL.set(0);
    }

    /** 供快照遍历的稳定键集（测试/诊断用）。 */
    static List<String> trackedTools() {
        return List.copyOf(PER_TOOL.keySet());
    }
}
