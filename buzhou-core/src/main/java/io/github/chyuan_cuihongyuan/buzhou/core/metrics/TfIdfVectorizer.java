package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TF-IDF 向量化（spec 9019 / W9039 / impl 2372）——TF-IDF 思想
 * （Salton 1988——scikit-learn TfidfVectorizer/Solr/ES 检索权重
 * 同源）：**词频 × 逆文档频率——常见词压权、鉴别词抬权的稀疏
 * 文档向量**——词袋计数（无鉴别力）与纯语义嵌入（黑箱）的
 * 经典中间形态。sklearn 平滑公式：tf=count/|doc|，
 * idf=ln((1+N)/(1+df))+1（无除零、空词表稳定）；词表全局
 * 升序（向量位序完全确定）；cosine 便利面（自比恒 1、
 * 无公共词正交）；null/空文档集 fail-fast。
 *
 * <p>与 Bm25Ranker（metrics 域）同域不同面：查询相关性打分 vs
 * 文档向量化（后者可任意复用余弦/聚类）；与 InvertedIndex
 * 互补：倒排定位 vs 权重表征。
 */
public final class TfIdfVectorizer {

    private final List<String> vocabulary;
    private final Map<String, Integer> termIndex;
    private final double[] idf;

    private TfIdfVectorizer(List<String> vocabulary, Map<String, Integer> termIndex, double[] idf) {
        this.vocabulary = vocabulary;
        this.termIndex = termIndex;
        this.idf = idf;
    }

    /**
     * 建向量化器（docs = 分词后的文档序列）。
     *
     * @throws IllegalArgumentException null/空文档集、null 文档/词
     */
    public static TfIdfVectorizer of(List<List<String>> docs) {
        if (docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("文档集非空（建词表面）");
        }
        Map<String, Integer> docFrequency = new TreeMap<>();
        for (List<String> doc : docs) {
            if (doc == null) {
                throw new IllegalArgumentException("文档非空引用");
            }
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String term : doc) {
                if (term == null) {
                    throw new IllegalArgumentException("词非空引用");
                }
                seen.add(term);
            }
            for (String term : seen) {
                docFrequency.merge(term, 1, Integer::sum);
            }
        }
        List<String> vocabulary = new ArrayList<>(docFrequency.keySet());
        Map<String, Integer> termIndex = new HashMap<>();
        for (int i = 0; i < vocabulary.size(); i++) {
            termIndex.put(vocabulary.get(i), i);
        }
        double[] idf = new double[vocabulary.size()];
        for (int i = 0; i < vocabulary.size(); i++) {
            idf[i] = Math.log((1.0 + docs.size()) / (1.0 + docFrequency.get(vocabulary.get(i)))) + 1.0;
        }
        return new TfIdfVectorizer(vocabulary, termIndex, idf);
    }

    /** 全局词表（升序——向量位序契约）。 */
    public List<String> vocabulary() {
        return vocabulary;
    }

    /** 词的平滑逆文档频率。 */
    public double idf(String term) {
        Integer index = termIndex.get(term);
        if (index == null) {
            throw new IllegalArgumentException("词表外词（" + term + "）——向量位序契约");
        }
        return idf[index];
    }

    /** 文档的 TF-IDF 稀疏向量（位序 = 词表升序；未知词忽略）。 */
    public double[] vector(List<String> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String term : tokens) {
            counts.merge(term, 1, Integer::sum);
        }
        double[] vector = new double[vocabulary.size()];
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Integer index = termIndex.get(entry.getKey());
            if (index != null) {
                double tf = entry.getValue() / (double) tokens.size();
                vector[index] = tf * idf[index];
            }
        }
        return vector;
    }

    /** 余弦相似度（自比恒 1；零向量与任意 = 0）。 */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("向量等长非空（余弦契约）");
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
