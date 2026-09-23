package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 区间树 stabbing 查询（spec 5022 / T6145 / impl 2173）——
 * 居中区间树思想（CLRS interval tree）：区间按中点分桶递归
 * 建树——含中点者入本节点列表、全在左/右者下推子树；
 * `stabbing(point)` 按点与中点关系走子树 + 节点列表过滤——
 * O(log n + k) 命中所有包含该点的区间（结果 start,end 字典序
 * 确定性）。全量线性扫（每查 O(n)）的病解。
 *
 * <p>静态构建口径（动态插入/删除不在本件）。与
 * SweepLineIntervals 同族不同面：扫线全序列 vs 树 stabbing
 * 点查。
 */
public final class IntervalTree {

    /**
     * 闭区间。
     *
     * @param start 起点（≤ end）
     * @param end 终点
     */
    public record Interval(long start, long end) {

        public Interval {
            if (start > end) {
                throw new IllegalArgumentException("区间倒置：" + start + ">" + end);
            }
        }

        boolean contains(long point) {
            return start <= point && point <= end;
        }
    }

    private static final class Node {
        private final long midpoint;
        private final List<Interval> covering;
        private final Node left;
        private final Node right;

        private Node(long midpoint, List<Interval> covering, Node left, Node right) {
            this.midpoint = midpoint;
            this.covering = covering;
            this.left = left;
            this.right = right;
        }
    }

    private final Node root;
    private final int size;

    /** 定构（静态构建；倒置区间 fail-fast；空集合法）。 */
    public IntervalTree(List<Interval> intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("区间集非 null");
        }
        intervals.forEach(interval -> {
            if (interval.start() > interval.end()) {
                throw new IllegalArgumentException("区间倒置：" + interval);
            }
        });
        this.size = intervals.size();
        this.root = build(new ArrayList<>(intervals));
    }

    /** 点 stabbing 查询（结果按 start,end 字典序——确定性）。 */
    public List<Interval> stabbing(long point) {
        List<Interval> hits = new ArrayList<>();
        collect(root, point, hits);
        hits.sort(Comparator.comparingLong(Interval::start).thenComparingLong(Interval::end));
        return hits;
    }

    /** 区间数读数。 */
    public int size() {
        return size;
    }

    private static void collect(Node node, long point, List<Interval> hits) {
        if (node == null) {
            return;
        }
        if (point == node.midpoint) {
            hits.addAll(node.covering);
            return;
        }
        if (point < node.midpoint) {
            for (Interval interval : node.covering) {
                if (interval.contains(point)) {
                    hits.add(interval);
                }
            }
            collect(node.left, point, hits);
            return;
        }
        for (Interval interval : node.covering) {
            if (interval.contains(point)) {
                hits.add(interval);
            }
        }
        collect(node.right, point, hits);
    }

    private static Node build(List<Interval> intervals) {
        if (intervals.isEmpty()) {
            return null;
        }
        long midpoint = medianMidpoint(intervals);
        List<Interval> covering = new ArrayList<>();
        List<Interval> goLeft = new ArrayList<>();
        List<Interval> goRight = new ArrayList<>();
        for (Interval interval : intervals) {
            if (interval.start() <= midpoint && midpoint <= interval.end()) {
                covering.add(interval);
            } else if (interval.end() < midpoint) {
                goLeft.add(interval);
            } else {
                goRight.add(interval);
            }
        }
        if (covering.size() == intervals.size()) {
            return new Node(midpoint, sorted(covering), null, null);   // 全覆盖——叶子
        }
        return new Node(midpoint, sorted(covering), build(goLeft), build(goRight));
    }

    /** 区间中点的中位数（确定性中点）。 */
    private static long medianMidpoint(List<Interval> intervals) {
        List<Long> midpoints = new ArrayList<>(intervals.size());
        for (Interval interval : intervals) {
            midpoints.add(Math.floorDiv(interval.start(), 2) + Math.floorDiv(interval.end(), 2)
                    + (Math.floorMod(interval.start(), 2) + Math.floorMod(interval.end(), 2)) / 2);
        }
        midpoints.sort(Long::compare);
        return midpoints.get(midpoints.size() / 2);
    }

    private static List<Interval> sorted(List<Interval> intervals) {
        List<Interval> copy = new ArrayList<>(intervals);
        copy.sort(Comparator.comparingLong(Interval::start).thenComparingLong(Interval::end));
        return copy;
    }
}
