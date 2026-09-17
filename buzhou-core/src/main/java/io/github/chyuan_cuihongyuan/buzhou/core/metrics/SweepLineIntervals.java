package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 扫线最大并发（spec 3013 / T5027 / impl 2014）——计算几何扫线
 * 思想（事件点 +1/−1 计数）：区间集的最大同时在档数及其首发点
 * ——并发峰值（同时活跃会话/在途工具调用/占用配额）的单遍精确
 * 口径。半开区间 [start,end) 语义：**相接不算并发**（前尾=后头
 * 时先处理 −1 再处理 +1——闭区间对相邻段重复计数的根治）；同峰
 * 取最早首发点。
 *
 * <p>纯函数静态件；空集诚实（maxConcurrent=0、atPoint=
 * {@link #NO_PEAK_POINT} 哨兵）；start&gt;end fail-fast；零宽
 * [x,x) 贡献为零（合法——空占位）。
 */
public final class SweepLineIntervals {

    /** 空集/零峰值的首发点哨兵（无峰值可指）。 */
    public static final long NO_PEAK_POINT = Long.MIN_VALUE;

    /** 半开区间 [start,end)。 */
    public record Interval(long start, long end) {
    }

    /** 扫线结果：峰值并发+首发点+区间总数。 */
    public record SweepResult(int maxConcurrent, long atPoint, int intervals) {
    }

    private SweepLineIntervals() {
    }

    /** 单遍扫线：事件按时间排（同刻 −1 先于 +1——相接不算并发）。 */
    public static SweepResult sweep(List<Interval> intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("intervals 非空");
        }
        record Event(long time, int delta) {
        }
        List<Event> events = new ArrayList<>(intervals.size() * 2);
        for (Interval interval : intervals) {
            if (interval.start() > interval.end()) {
                throw new IllegalArgumentException("start ≤ end 破缺：" + interval);
            }
            events.add(new Event(interval.start(), 1));
            events.add(new Event(interval.end(), -1));
        }
        events.sort(Comparator.comparingLong(Event::time).thenComparingInt(Event::delta));
        int running = 0;
        int max = 0;
        long atPoint = NO_PEAK_POINT;
        for (Event event : events) {
            running += event.delta();
            if (running > max) {
                max = running;
                atPoint = event.time();
            }
        }
        return new SweepResult(max, atPoint, intervals.size());
    }
}
