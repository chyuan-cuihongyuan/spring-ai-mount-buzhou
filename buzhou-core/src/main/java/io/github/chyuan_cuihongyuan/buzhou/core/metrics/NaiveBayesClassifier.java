package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 多项式朴素贝叶斯（spec 10042 / X10085 / impl 2445）——McCallum–Nigam 1998
 * 思想（scikit-learn MultinomialNB 同源）：**类词计数 + Laplace α=1 平滑
 * θ=(count+α)/(total+αV) 全对数化 + 对数后验 argmax**——条件独立假设下的
 * 词袋文本分类生成面（对数域防连乘下溢）。
 *
 * <p>词 ID 域 [0,V)：V=语料见过的最大词 ID+1（未见词不计入 V、按平滑兜底）；
 * 平局取首类（确定性口径）。null/标签越界/负词 ID/非正类数 fail-fast；
 * 模型不可变。
 */
public final class NaiveBayesClassifier {

    /** Laplace 平滑强度。 */
    private static final double LAPLACE_ALPHA = 1.0;

    private final double[] logPriors;
    private final double[][] logTheta;
    private final double[] logUnseen;
    private final int vocabularySize;

    private NaiveBayesClassifier(double[] logPriors, double[][] logTheta,
            double[] logUnseen, int vocabularySize) {
        this.logPriors = logPriors;
        this.logTheta = logTheta;
        this.logUnseen = logUnseen;
        this.vocabularySize = vocabularySize;
    }

    /**
     * 拟合。
     *
     * @param documents 词袋文档（每行词 ID 数组，可变长）
     * @param labels 类标签（[0,classCount)）
     * @param classCount 类数（≥1）
     * @throws IllegalArgumentException null/长度不配/标签越界/负词 ID/非正类数/空语料
     */
    public static NaiveBayesClassifier fit(int[][] documents, int[] labels,
            int classCount) {
        validate(documents, labels, classCount);
        int vocabularySize = 0;
        for (int[] document : documents) {
            for (int word : document) {
                vocabularySize = Math.max(vocabularySize, word + 1);
            }
        }
        int[][] classWordCounts = new int[classCount][];
        int[] classTotals = new int[classCount];
        int[] classDocCounts = new int[classCount];
        for (int c = 0; c < classCount; c++) {
            classWordCounts[c] = new int[vocabularySize];
        }
        for (int d = 0; d < documents.length; d++) {
            int label = labels[d];
            classDocCounts[label]++;
            for (int word : documents[d]) {
                classWordCounts[label][word]++;
                classTotals[label]++;
            }
        }
        double[] logPriors = new double[classCount];
        double[][] logTheta = new double[classCount][];
        double[] logUnseen = new double[classCount];
        for (int c = 0; c < classCount; c++) {
            logPriors[c] = Math.log((double) classDocCounts[c] / documents.length);
            logTheta[c] = new double[vocabularySize];
            double denominator = classTotals[c] + LAPLACE_ALPHA * vocabularySize;
            for (int w = 0; w < vocabularySize; w++) {
                logTheta[c][w] = Math.log(
                        (classWordCounts[c][w] + LAPLACE_ALPHA) / denominator);
            }
            logUnseen[c] = Math.log(LAPLACE_ALPHA / denominator);
        }
        return new NaiveBayesClassifier(logPriors, logTheta, logUnseen, vocabularySize);
    }

    /**
     * 预测（对数后验 argmax，平局取首类）。
     *
     * @throws IllegalArgumentException null/负词 ID
     */
    public int predict(int[] document) {
        if (document == null) {
            throw new IllegalArgumentException("文档非 null");
        }
        int best = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < logPriors.length; c++) {
            double score = logPriors[c];
            for (int word : document) {
                if (word < 0) {
                    throw new IllegalArgumentException("词 ID 非负（实际 " + word + "）");
                }
                score += word < vocabularySize ? logTheta[c][word] : logUnseen[c];
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private static void validate(int[][] documents, int[] labels, int classCount) {
        if (classCount < 1) {
            throw new IllegalArgumentException("类数为正（实际 " + classCount + "）");
        }
        if (documents == null || labels == null) {
            throw new IllegalArgumentException("语料/标签非 null");
        }
        if (documents.length == 0) {
            throw new IllegalArgumentException("语料非空");
        }
        if (documents.length != labels.length) {
            throw new IllegalArgumentException("长度不配（docs=" + documents.length
                    + " labels=" + labels.length + "）");
        }
        for (int d = 0; d < documents.length; d++) {
            if (documents[d] == null) {
                throw new IllegalArgumentException("文档非 null（第 " + d + " 篇）");
            }
            if (labels[d] < 0 || labels[d] >= classCount) {
                throw new IllegalArgumentException("标签越界（第 " + d + " 篇 "
                        + labels[d] + "，类数 " + classCount + "）");
            }
            for (int word : documents[d]) {
                if (word < 0) {
                    throw new IllegalArgumentException("词 ID 非负（第 " + d + " 篇 "
                            + word + "）");
                }
            }
        }
    }
}
