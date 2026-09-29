package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.Arrays;
import java.util.Random;

/**
 * K-Means 聚类（spec 8030 / V8061 / impl 2332）——
 * MacQueen 1967/Lloyd 迭代思想：**k-means++ 距离平方加权
 * 播种 + 指派-更新交替至指派不变**——层次聚类 O(n³)（规模
 * 放大）的病解。空簇重播（最远点）；maxIter 双停；种子化
 * （同种子同结果完全确定）；维度一致/空集/k 越域 fail-fast。
 *
 * <p>与 DbScanClusterer（spec 8031）同族不同面：凸球簇预知
 * k vs 任意形状密度可达。
 */
public final class KMeansClustering {

    private KMeansClustering() {
    }

    /** 聚类：返回每点簇标签（0..k-1；维度一致/空集/k 越域 fail-fast）。 */
    public static int[] assign(double[][] points, int k, long seed, int maxIter) {
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("点集非空");
        }
        if (k < 1 || k > points.length) {
            throw new IllegalArgumentException("k ∈ [1,n]（实际 " + k + "）");
        }
        if (maxIter < 1) {
            throw new IllegalArgumentException("maxIter 非负（实际 " + maxIter + "）");
        }
        int dimensions = points[0].length;
        for (double[] point : points) {
            if (point == null || point.length != dimensions) {
                throw new IllegalArgumentException("维度一致（实际 " + (point == null ? "null" : point.length)
                        + " vs " + dimensions + "）");
            }
        }
        Random random = new Random(seed);
        double[][] centroids = seedCentroids(points, k, random);
        int[] labels = new int[points.length];
        Arrays.fill(labels, -1);
        for (int iteration = 0; iteration < maxIter; iteration++) {
            boolean changed = false;
            for (int i = 0; i < points.length; i++) {
                int nearest = nearest(points[i], centroids);
                if (labels[i] != nearest) {
                    labels[i] = nearest;
                    changed = true;
                }
            }
            if (!changed) {
                return labels;
            }
            centroids = updateCentroids(points, labels, k, random);
        }
        return labels;
    }

    /** 惯性（各点到所属簇质心距离平方和——审计面）。 */
    public static double inertia(double[][] points, int[] labels, int k) {
        double total = 0;
        for (int c = 0; c < k; c++) {
            double[] centroid = null;
            int count = 0;
            for (int i = 0; i < points.length; i++) {
                if (labels[i] != c) {
                    continue;
                }
                if (centroid == null) {
                    centroid = new double[points[i].length];
                }
                for (int d = 0; d < centroid.length; d++) {
                    centroid[d] += points[i][d];
                }
                count++;
            }
            if (count == 0) {
                continue;
            }
            for (int d = 0; d < centroid.length; d++) {
                centroid[d] /= count;
            }
            for (int i = 0; i < points.length; i++) {
                if (labels[i] == c) {
                    total += squaredDistance(points[i], centroid);
                }
            }
        }
        return total;
    }

    private static double[][] seedCentroids(double[][] points, int k, Random random) {
        double[][] centroids = new double[k][];
        centroids[0] = points[random.nextInt(points.length)].clone();
        double[] distance = new double[points.length];
        Arrays.fill(distance, Double.MAX_VALUE);
        for (int c = 1; c < k; c++) {
            double sum = 0;
            for (int i = 0; i < points.length; i++) {
                double d = squaredDistance(points[i], centroids[c - 1]);
                distance[i] = Math.min(distance[i], d);
                sum += distance[i];
            }
            if (sum == 0) {
                centroids[c] = points[random.nextInt(points.length)].clone();
                continue;
            }
            double pick = random.nextDouble() * sum;
            int chosen = points.length - 1;
            for (int i = 0; i < points.length; i++) {
                pick -= distance[i];
                if (pick <= 0) {
                    chosen = i;
                    break;
                }
            }
            centroids[c] = points[chosen].clone();
        }
        return centroids;
    }

    private static double[][] updateCentroids(double[][] points, int[] labels, int k, Random random) {
        double[][] centroids = new double[k][];
        for (int c = 0; c < k; c++) {
            double[] sum = null;
            int count = 0;
            for (int i = 0; i < points.length; i++) {
                if (labels[i] != c) {
                    continue;
                }
                if (sum == null) {
                    sum = new double[points[i].length];
                }
                for (int d = 0; d < sum.length; d++) {
                    sum[d] += points[i][d];
                }
                count++;
            }
            if (sum == null || count == 0) {
                centroids[c] = points[random.nextInt(points.length)].clone();
                continue;
            }
            for (int d = 0; d < sum.length; d++) {
                sum[d] /= count;
            }
            centroids[c] = sum;
        }
        return centroids;
    }

    private static int nearest(double[] point, double[][] centroids) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int c = 0; c < centroids.length; c++) {
            double d = squaredDistance(point, centroids[c]);
            if (d < bestDistance) {
                bestDistance = d;
                best = c;
            }
        }
        return best;
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
