package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 区间合并与空闲缝隙（spec 1868 / T2937 / impl 1469）——日历调度惯例
 *（Google Calendar busy/free）：忙时段合并（重叠/相邻/嵌套归一——
 *「9-10 与 10-11」就是「9-11」），窗口内找**空闲缝隙**（可排新任务的
  档期）。映射到 turn 时间线：工具占用段合并后剩余可调度窗一目了然。
 *
 * <p>纯函数零状态、确定性（排序后扫描）；乱序输入容忍。
 */
public final class IntervalSchedule {

    private IntervalSchedule() {
    }

    /** 区间契约：start ≤ end（零长区间合法——瞬时占用）。 */
    public record Interval(long startMillis, long endMillis) {

        public Interval {
            if (startMillis > endMillis) {
                throw new IllegalArgumentException(String.format(
                        "非法区间：start=%d > end=%d", startMillis, endMillis));
            }
        }

        boolean overlapsOrTouches(Interval other) {
            return other.startMillis <= endMillis;
        }
    }

    /**
     * 忙时段合并：按 start 排序后扫描——重叠/相邻（next.start ≤ cur.end）
     * 归一取 max end。null 按空表；输入乱序容忍（内部排序）。
     */
    public static List<Interval> merge(List<Interval> intervals) {
        List<Interval> window = validated(intervals);
        window.sort(Comparator.comparingLong(Interval::startMillis));
        List<Interval> merged = new ArrayList<>();
        for (Interval current : window) {
            if (!merged.isEmpty()
                    && merged.get(merged.size() - 1).overlapsOrTouches(current)) {
                Interval last = merged.remove(merged.size() - 1);
                merged.add(new Interval(last.startMillis,
                        Math.max(last.endMillis, current.endMillis)));
            } else {
                merged.add(current);
            }
        }
        return List.copyOf(merged);
    }

    /**
     * 窗口内空闲缝隙：合并后在 [from, to] 内取缝（含首前/尾后与区间间）。
     * 契约：from ≤ to（fail-fast）；区间可越窗口界（缝只报窗口内部分）。
     */
    public static List<Interval> gaps(List<Interval> intervals,
                                      long fromMillis, long toMillis) {
        if (fromMillis > toMillis) {
            throw new IllegalArgumentException(String.format(
                    "非法窗口：from=%d > to=%d", fromMillis, toMillis));
        }
        List<Interval> merged = merge(intervals);
        List<Interval> gaps = new ArrayList<>();
        long cursor = fromMillis;
        for (Interval interval : merged) {
            if (interval.endMillis <= cursor) {
                continue; // 已越过（窗口前结束）
            }
            if (interval.startMillis > cursor) {
                long gapEnd = Math.min(interval.startMillis, toMillis);
                if (gapEnd > cursor) {
                    gaps.add(new Interval(cursor, gapEnd));
                }
            }
            cursor = Math.max(cursor, interval.endMillis);
            if (cursor >= toMillis) {
                return List.copyOf(gaps);
            }
        }
        if (cursor < toMillis) {
            gaps.add(new Interval(cursor, toMillis));
        }
        return List.copyOf(gaps);
    }

    private static List<Interval> validated(List<Interval> intervals) {
        if (intervals == null) {
            return new ArrayList<>();
        }
        List<Interval> copy = new ArrayList<>();
        for (Interval i : intervals) {
            if (i == null) {
                throw new IllegalArgumentException("区间不能为 null");
            }
            copy.add(i);
        }
        return copy;
    }
}
