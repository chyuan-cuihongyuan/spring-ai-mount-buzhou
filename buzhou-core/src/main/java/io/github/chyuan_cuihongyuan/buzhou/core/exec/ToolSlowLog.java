package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * impl-754 / spec 1001：工具慢调用榜（Redis SLOWLOG 借鉴——执行时长<b>严格大于</b>
 * 阈值 {@code slowlog-log-slower-than} 才入榜，有界 FIFO 环 {@code slowlog-max-len}，
 * 超限挤掉最旧；非严格 Top-K 排名）。
 *
 * <p>聚合面（spec 108 timer P95 / spec 700 ToolTimingAggregator）之外的<b>单次现场</b>：
 * 最近哪几次调用慢、慢在哪个工具、是否伴随失败。只记工具名不记入参（入参可能含
 * 敏感内容——红线纪律，与 SLOWLOG 记 argv 的差异有意为之）。
 *
 * <p>接线点：{@code HookedToolCallback} 与 timer 同点（每调用至多一次环操作；
 * 不达阈值只付一次 volatile 比较——热路径零担）。
 */
public final class ToolSlowLog {

    /** 默认入榜阈值（毫秒）：执行时长严格大于该值才记录。 */
    public static final long DEFAULT_SLOWER_THAN_MILLIS = 1_000L;

    /** 有界环容量（SLOWLOG max-len 同义）。 */
    static final int CAPACITY = 32;

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static final Object LOCK = new Object();
    private static volatile long slowerThanNanos = DEFAULT_SLOWER_THAN_MILLIS * NANOS_PER_MILLI;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private ToolSlowLog() {
    }

    /**
     * 单次慢调用现场（不可变）。
     *
     * @param toolName      工具名
     * @param durationMillis 入榜时长（纳秒折毫秒截断）
     * @param epochMillis    入榜时刻（{@code System.currentTimeMillis()}）
     * @param failed         本次调用是否以异常收场（spec 108 outcome 同口径）
     */
    public record Entry(String toolName, long durationMillis, long epochMillis, boolean failed) {
    }

    /**
     * 记录一次工具调用时长：严格大于当前阈值才入榜（Redis 口径——恰好等于不入）。
     *
     * @param toolName     工具名
     * @param elapsedNanos 本次调用耗时（纳秒）
     * @param failed       是否异常收场
     */
    public static void record(String toolName, long elapsedNanos, boolean failed) {
        long threshold = slowerThanNanos;
        if (elapsedNanos <= threshold) {
            return;
        }
        Entry entry = new Entry(toolName, elapsedNanos / NANOS_PER_MILLI,
                System.currentTimeMillis(), failed);
        synchronized (LOCK) {
            ENTRIES.addFirst(entry);
            while (ENTRIES.size() > CAPACITY) {
                ENTRIES.removeLast();
            }
        }
    }

    /** 慢调用榜只读快照（新→旧；List.copyOf 不可变）。 */
    public static List<Entry> entries() {
        synchronized (LOCK) {
            return List.copyOf(ENTRIES);
        }
    }

    /**
     * 进程级入榜阈值（volatile——热路径读一次；设置后即时生效，不改已入榜条目）。
     */
    public static void configureThreshold(Duration slowerThan) {
        if (slowerThan == null || slowerThan.isNegative()) {
            throw new IllegalArgumentException("慢调用阈值必须非空且非负");
        }
        slowerThanNanos = slowerThan.toNanos();
    }

    /** 当前入榜阈值（毫秒）。 */
    public static long thresholdMillis() {
        return slowerThanNanos / NANOS_PER_MILLI;
    }

    /**
     * 清空榜单并恢复默认阈值（测试隔离注入点——BuzhouMetricsHolder 进程态先例；
     * 生产勿调）。
     */
    public static void reset() {
        slowerThanNanos = DEFAULT_SLOWER_THAN_MILLIS * NANOS_PER_MILLI;
        synchronized (LOCK) {
            ENTRIES.clear();
        }
    }
}
