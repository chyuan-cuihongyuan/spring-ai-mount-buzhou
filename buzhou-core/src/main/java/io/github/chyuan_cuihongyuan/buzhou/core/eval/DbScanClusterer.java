package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * DBSCAN 密度聚类（spec 8031 / V8063 / impl 2333）——
 * Ester 1996 思想：**ε 邻域 + minPts 核心点判定 + 密度可达
 * 扩张**——任意形状簇 + 噪声标记 -1——K-Means 强设凸球簇
 * 且需预知 k（月牙/环状数据撕裂）的病解。扩张队列顺序种子
 * 化（同输入同标签完全确定——边界点归先达簇）；ε≤0/minPts<1
 * fail-fast。
 *
 * <p>与 KMeansClustering（spec 8030）同族不同面：密度可达
 * 任意形状 vs 凸球簇预知 k。
 */
public final class DbScanClusterer {

    /** 噪声标签。 */
    public static final int NOISE = -1;

    private DbScanClusterer() {
    }

    /** 聚类：返回每点标签（0..簇数-1；噪声 -1；参数越域 fail-fast）。 */
    public static int[] cluster(double[][] points, double eps, int minPts) {
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("点集非空");
        }
        if (eps <= 0) {
            throw new IllegalArgumentException("ε 为正（实际 " + eps + "）");
        }
        if (minPts < 1) {
            throw new IllegalArgumentException("minPts ≥1（实际 " + minPts + "）");
        }
        double epsSquared = eps * eps;
        int[] labels = new int[points.length];
        java.util.Arrays.fill(labels, -2);
        int cluster = 0;
        for (int start = 0; start < points.length; start++) {
            if (labels[start] != -2) {
                continue;
            }
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            java.util.List<Integer> members = new java.util.ArrayList<>();
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            seen.add(start);
            while (!queue.isEmpty()) {
                int current = queue.poll();
                java.util.List<Integer> neighbors = regionQuery(points, current, epsSquared);
                members.add(current);
                if (neighbors.size() < minPts) {
                    continue;
                }
                for (int neighbor : neighbors) {
                    if (seen.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            boolean core = regionQuery(points, start, epsSquared).size() >= minPts;
            if (!core) {
                for (int member : members) {
                    labels[member] = NOISE;
                }
                continue;
            }
            for (int member : members) {
                labels[member] = cluster;
            }
            cluster++;
        }
        return labels;
    }

    private static java.util.List<Integer> regionQuery(double[][] points, int center, double epsSquared) {
        java.util.List<Integer> neighbors = new java.util.ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            if (squaredDistance(points[center], points[i]) <= epsSquared) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    private static double squaredDistance(double[] a, double[] b) {
        double sum = 0;
        for (int d = 0; d < a.length; d++) {
            double diff = a[d] - b[d];
            sum += diff * diff;
        }
        return sum;
    }
}
