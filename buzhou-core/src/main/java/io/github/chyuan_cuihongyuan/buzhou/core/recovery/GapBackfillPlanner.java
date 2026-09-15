package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 缺口回填计划器（spec 1815 / T2831 / impl 1416）——Kafka offset 补填 /
 * Prometheus backfill 思想：期望连续序号区间与实际在位集合之差即**缺口
 * 区间清单**——停机/断流后重放不重不漏的依据是「先知道缺哪段」。缺口
 * 合并为区间（而非逐点）让回填可分批、可并行、最大缺口直读（重放瓶颈
 * 单段长度）。
 *
 * <p>纯函数零状态、只计划不执行（回填动作归宿主）；在位序号乱序/重复
 * 容忍（内部排序去重）。
 */
public final class GapBackfillPlanner {

    private GapBackfillPlanner() {
    }

    /** 单缺口闭区间 [from, to]；span = to − from + 1。 */
    public record Gap(long fromInclusive, long toInclusive) {

        public long span() {
            return toInclusive - fromInclusive + 1;
        }
    }

    /**
     * @param expectedCount 期望区间长度（to − from + 1）
     * @param presentCount  去重在位数
     * @param gaps          缺口区间清单（升序连续，无相邻合并漏）
     * @param largestGapSpan 最大单缺口长度（无缺口 0）
     */
    public record BackfillPlan(long expectedCount, long presentCount,
                               List<Gap> gaps, long largestGapSpan) {

        /** 缺失率 = (expected−present)/expected（期望 0 -1 哨兵）。 */
        public double missingRatio() {
            return expectedCount == 0 ? -1d
                    : (double) (expectedCount - presentCount) / expectedCount;
        }

        /** 完整性：无缺口即完整。 */
        public boolean complete() {
            return gaps.isEmpty();
        }
    }

    /**
     * 计划入口。契约：from ≤ to（空区间用 from = to + 1 表达）；在位值须
     * 落在 [from, to]（越界 fail-fast——脏事实不吞）；null 按空集。
     */
    public static BackfillPlan plan(long from, long to, List<Long> presentSeqs) {
        if (from > to + 1) {
            throw new IllegalArgumentException(
                    "非法区间：from=" + from + ", to=" + to + "（要求 from ≤ to+1）");
        }
        Set<Long> present = new HashSet<>();
        if (presentSeqs != null) {
            for (Long v : presentSeqs) {
                if (v == null) {
                    throw new IllegalArgumentException("在位序号不能为 null");
                }
                if (v < from || v > to) {
                    throw new IllegalArgumentException(
                            "在位序号越界：" + v + "（区间 [" + from + ", " + to + "]）");
                }
                present.add(v);
            }
        }
        List<Long> sorted = new ArrayList<>(present);
        sorted.sort(Comparator.naturalOrder());
        List<Gap> gaps = new ArrayList<>();
        long largest = 0;
        long cursor = from;
        for (long v : sorted) {
            if (v > cursor) {
                Gap gap = new Gap(cursor, v - 1);
                gaps.add(gap);
                largest = Math.max(largest, gap.span());
            }
            cursor = Math.max(cursor, v + 1);
        }
        if (cursor <= to) {
            Gap gap = new Gap(cursor, to);
            gaps.add(gap);
            largest = Math.max(largest, gap.span());
        }
        return new BackfillPlan(to - from + 1, present.size(), List.copyOf(gaps), largest);
    }
}
