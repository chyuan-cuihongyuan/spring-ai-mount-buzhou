package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;

import java.util.List;

/**
 * CART 基尼分类树（spec 10046 / X10093 / impl 2449）——Breiman 1984 思想
 * （scikit-learn DecisionTreeClassifier 同源）：**逐特征排序去重相邻中点为
 * 阈值候选、加权基尼不升即贪心分裂（零增益允许——XOR 根节点零增益首裂面，
 * sklearn 同口径；先到先得确定序）**；停机三则（纯/达深度限/样本不足
 * 2·最小叶）；叶取多数类（平局取首类确定口径）。
 *
 * <p>嵌套 Node 树+predict 走树；null/锯齿行/标签越界/超参越域 fail-fast；
 * 同输入复算确定。
 */
public final class DecisionTreeCart {

    private final Node root;

    /** 树节点（叶持多数类，内点持分裂面）。 */
    private static final class Node {
        private final int leafClass;
        private final int featureIndex;
        private final double threshold;
        private final Node left;
        private final Node right;

        private Node(int leafClass, int featureIndex, double threshold,
                Node left, Node right) {
            this.leafClass = leafClass;
            this.featureIndex = featureIndex;
            this.threshold = threshold;
            this.left = left;
            this.right = right;
        }
    }

    private DecisionTreeCart(Node root) {
        this.root = root;
    }

    /**
     * 拟合。
     *
     * @param features 特征行（等长）
     * @param labels 类标签（[0,classCount)）
     * @param classCount 类数（≥2）
     * @param maxDepth 深度限（≥1）
     * @param minSamplesLeaf 最小叶样本（≥1）
     * @throws IllegalArgumentException null/锯齿行/标签越界/超参越域/空集
     */
    public static DecisionTreeCart fit(double[][] features, int[] labels,
            int classCount, int maxDepth, int minSamplesLeaf) {
        validate(features, labels, classCount, maxDepth, minSamplesLeaf);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < features.length; i++) {
            indices.add(i);
        }
        Node root = build(features, labels, classCount, indices, 1, maxDepth,
                minSamplesLeaf);
        return new DecisionTreeCart(root);
    }

    /**
     * 预测（走树至叶多数类）。
     *
     * @throws IllegalArgumentException null/维数不配
     */
    public int predict(double[] features) {
        if (features == null) {
            throw new IllegalArgumentException("特征非 null");
        }
        Node node = root;
        while (node.left != null) {
            node = features[node.featureIndex] <= node.threshold
                    ? node.left : node.right;
        }
        return node.leafClass;
    }

    private static Node build(double[][] features, int[] labels, int classCount,
            List<Integer> indices, int depth, int maxDepth, int minSamplesLeaf) {
        int majority = majorityClass(labels, indices, classCount);
        if (depth > maxDepth || indices.size() < 2 * minSamplesLeaf
                || isPure(labels, indices)) {
            return new Node(majority, -1, Double.NaN, null, null);
        }
        Split best = bestSplit(features, labels, classCount, indices, minSamplesLeaf);
        if (best == null) {
            return new Node(majority, -1, Double.NaN, null, null);
        }
        List<Integer> leftIndices = new ArrayList<>();
        List<Integer> rightIndices = new ArrayList<>();
        for (int i : indices) {
            if (features[i][best.featureIndex] <= best.threshold) {
                leftIndices.add(i);
            } else {
                rightIndices.add(i);
            }
        }
        return new Node(-1, best.featureIndex, best.threshold,
                build(features, labels, classCount, leftIndices, depth + 1,
                        maxDepth, minSamplesLeaf),
                build(features, labels, classCount, rightIndices, depth + 1,
                        maxDepth, minSamplesLeaf));
    }

    private static Split bestSplit(double[][] features, int[] labels, int classCount,
            List<Integer> indices, int minSamplesLeaf) {
        double parentGini = gini(labels, indices, classCount);
        Split best = null;
        double bestGini = parentGini;
        int dimension = features[0].length;
        for (int f = 0; f < dimension; f++) {
            final int feature = f;
            double[] sorted = indices.stream().mapToDouble(i -> features[i][feature])
                    .sorted().distinct().toArray();
            for (int t = 0; t + 1 < sorted.length; t++) {
                double threshold = (sorted[t] + sorted[t + 1]) / 2.0;
                List<Integer> left = new ArrayList<>();
                List<Integer> right = new ArrayList<>();
                for (int i : indices) {
                    if (features[i][f] <= threshold) {
                        left.add(i);
                    } else {
                        right.add(i);
                    }
                }
                if (left.size() < minSamplesLeaf || right.size() < minSamplesLeaf) {
                    continue;
                }
                double weighted = ((double) left.size() * gini(labels, left, classCount)
                        + right.size() * gini(labels, right, classCount))
                        / indices.size();
                // 不升即裂（零增益允许——XOR 根节点面；sklearn 同口径），
                // 先到先得保持确定序
                if (weighted < bestGini
                        || (best == null && weighted <= parentGini)) {
                    bestGini = weighted;
                    best = new Split(f, threshold);
                }
            }
        }
        return best;
    }

    private record Split(int featureIndex, double threshold) {
    }

    private static double gini(int[] labels, List<Integer> indices, int classCount) {
        int[] counts = new int[classCount];
        for (int i : indices) {
            counts[labels[i]]++;
        }
        double impurity = 1.0;
        for (int count : counts) {
            double p = (double) count / indices.size();
            impurity -= p * p;
        }
        return impurity;
    }

    private static boolean isPure(int[] labels, List<Integer> indices) {
        int first = labels[indices.get(0)];
        for (int i : indices) {
            if (labels[i] != first) {
                return false;
            }
        }
        return true;
    }

    private static int majorityClass(int[] labels, List<Integer> indices,
            int classCount) {
        int[] counts = new int[classCount];
        for (int i : indices) {
            counts[labels[i]]++;
        }
        int best = 0;
        for (int c = 1; c < classCount; c++) {
            if (counts[c] > counts[best]) {
                best = c;
            }
        }
        return best;
    }

    private static void validate(double[][] features, int[] labels, int classCount,
            int maxDepth, int minSamplesLeaf) {
        if (classCount < 2) {
            throw new IllegalArgumentException("类数 ≥2（实际 " + classCount + "）");
        }
        if (maxDepth < 1) {
            throw new IllegalArgumentException("深度限为正（实际 " + maxDepth + "）");
        }
        if (minSamplesLeaf < 1) {
            throw new IllegalArgumentException("最小叶样本为正（实际 " + minSamplesLeaf + "）");
        }
        if (features == null || labels == null) {
            throw new IllegalArgumentException("特征/标签非 null");
        }
        if (features.length == 0) {
            throw new IllegalArgumentException("样本集非空");
        }
        if (features.length != labels.length) {
            throw new IllegalArgumentException("长度不配（X=" + features.length
                    + " y=" + labels.length + "）");
        }
        int dimension = features[0].length;
        for (int i = 0; i < features.length; i++) {
            if (features[i] == null || features[i].length != dimension) {
                throw new IllegalArgumentException("等长特征行（第 " + i + " 行）");
            }
            if (labels[i] < 0 || labels[i] >= classCount) {
                throw new IllegalArgumentException("标签越界（第 " + i + " 行 "
                        + labels[i] + "）");
            }
            for (double v : features[i]) {
                if (!Double.isFinite(v)) {
                    throw new IllegalArgumentException("特征非有限（第 " + i + " 行）");
                }
            }
        }
    }
}
