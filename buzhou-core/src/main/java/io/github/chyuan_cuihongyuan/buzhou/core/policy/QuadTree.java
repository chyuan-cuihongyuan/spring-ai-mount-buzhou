package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * QuadTree 四叉树（spec 6020 / T6239 / impl 2220）——
 * 地理围栏/空间索引四叉树思想：**象限递归分割的点域树**——
 * 桶容量超限即四分（容量 4），点按象限（x≥cx、y≥cy 的
 * 2 bit 组合）下放；区域查询按「矩形与节点界相交」剪枝
 * （O(log n + 命中数) 均摊）——每查询全点集线性扫（O(n)
 * 放大）的病解。查询结果 (x,y) 字典序 canonical 输出。
 *
 * <p>与 KdTree（spec 6019）同族不同面：象限桶区域查询 vs
 * 轴分割最近邻。根界定构（越界插入 fail-fast——确定性）。
 */
public final class QuadTree {

    /** 点（long 坐标）。 */
    public record Point(long x, long y) {
    }

    /** 查询矩形（x1≤x2, y1≤y2，闭区间）。 */
    public record Rect(long x1, long y1, long x2, long y2) {
    }

    private static final int BUCKET_CAPACITY = 4;

    private final long minX;
    private final long minY;
    private final long maxX;
    private final long maxY;
    private Node root;
    private int size;

    private QuadTree(long minX, long minY, long maxX, long maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /** 以根界建树（界倒置 fail-fast）。 */
    public static QuadTree overBounds(long minX, long minY, long maxX, long maxY) {
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("根界倒置");
        }
        return new QuadTree(minX, minY, maxX, maxY);
    }

    /** 插入点（越界 fail-fast；重复点允许）。 */
    public void insert(long x, long y) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            throw new IllegalArgumentException("点越出根界: (" + x + ", " + y + ")");
        }
        root = insert(root, minX, minY, maxX, maxY, x, y);
        size++;
    }

    /** 区域查询（矩形内全部点，字典序 canonical）。 */
    public List<Point> query(Rect rect) {
        if (rect.x1() > rect.x2() || rect.y1() > rect.y2()) {
            throw new IllegalArgumentException("查询矩形倒置");
        }
        List<Point> out = new ArrayList<>();
        collect(root, rect, out);
        out.sort((a, b) -> {
            int byX = Long.compare(a.x(), b.x());
            return byX != 0 ? byX : Long.compare(a.y(), b.y());
        });
        return out;
    }

    /** 点数读数。 */
    public int size() {
        return size;
    }

    private static final class Node {
        final long minX;
        final long minY;
        final long maxX;
        final long maxY;
        final List<Point> bucket = new ArrayList<>();
        Node[] children;

        Node(long minX, long minY, long maxX, long maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }

    private Node insert(Node node, long minX, long minY, long maxX, long maxY, long x, long y) {
        if (node == null) {
            node = new Node(minX, minY, maxX, maxY);
        }
        if (node.children == null) {
            node.bucket.add(new Point(x, y));
            if (node.bucket.size() > BUCKET_CAPACITY
                    && node.maxX > node.minX && node.maxY > node.minY) {
                subdivide(node);
            }
            return node;
        }
        long midX = (node.minX + node.maxX) >>> 1;
        long midY = (node.minY + node.maxY) >>> 1;
        int child = childIndex(x, y, midX, midY);
        node.children[child] = insert(node.children[child],
                childBounds(node, child)[0], childBounds(node, child)[1],
                childBounds(node, child)[2], childBounds(node, child)[3], x, y);
        return node;
    }

    private void subdivide(Node node) {
        long midX = (node.minX + node.maxX) >>> 1;
        long midY = (node.minY + node.maxY) >>> 1;
        node.children = new Node[4];
        List<Point> points = new ArrayList<>(node.bucket);
        node.bucket.clear();
        for (Point p : points) {
            int child = childIndex(p.x(), p.y(), midX, midY);
            long[] bounds = childBounds(node, child);
            node.children[child] = insert(node.children[child],
                    bounds[0], bounds[1], bounds[2], bounds[3], p.x(), p.y());
        }
    }

    private int childIndex(long x, long y, long midX, long midY) {
        return (x > midX ? 1 : 0) + (y > midY ? 2 : 0);
    }

    private long[] childBounds(Node node, int child) {
        long midX = (node.minX + node.maxX) >>> 1;
        long midY = (node.minY + node.maxY) >>> 1;
        return new long[]{
                (child & 1) == 1 ? midX + 1 : node.minX,
                (child & 2) == 2 ? midY + 1 : node.minY,
                (child & 1) == 1 ? node.maxX : midX,
                (child & 2) == 2 ? node.maxY : midY
        };
    }

    private void collect(Node node, Rect rect, List<Point> out) {
        if (node == null
                || rect.x1() > node.maxX || rect.x2() < node.minX
                || rect.y1() > node.maxY || rect.y2() < node.minY) {
            return;
        }
        for (Point p : node.bucket) {
            if (p.x() >= rect.x1() && p.x() <= rect.x2()
                    && p.y() >= rect.y1() && p.y() <= rect.y2()) {
                out.add(p);
            }
        }
        if (node.children != null) {
            for (Node child : node.children) {
                collect(child, rect, out);
            }
        }
    }
}
