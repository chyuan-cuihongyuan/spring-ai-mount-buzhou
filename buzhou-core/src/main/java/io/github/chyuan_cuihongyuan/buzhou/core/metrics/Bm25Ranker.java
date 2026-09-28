package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BM25 评分器（spec 7027 / U7255 / impl 2279）——Robertson &
 * Spärck Jones Okapi BM25 思想（Lucene/Elasticsearch 默认相关
 * 性）：**IDF 饱和 + 词频饱和 + 文档长度归一**——score =
 * Σ idf(q)·tf·(k1+1)/(tf+k1·(1−b+b·|d|/avgdl))——裸 TF 计数
 * （长文档刷分）的病解。参数 k1∈[0,∞)、b∈[0,1]（0=不归一，
 * 1=完全归一——明示域）。确定性：同语料同查询同分；并列
 * 按 docId 升序（canonical）。
 *
 * <p>与 InvertedIndex（同包）同族不同面：布尔召回 vs 相关性
 * 排序。
 */
public final class Bm25Ranker {

    private final Map<Long, List<String>> documents = new TreeMap<>();
    private final Map<String, Integer> docFrequency = new TreeMap<>();
    private double totalLength;

    /** 索引文档（重复 docId 幂等替换；null/空 fail-fast）。 */
    public void add(long docId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本非空");
        }
        unregister(docId);
        List<String> terms = tokenize(text);
        documents.put(docId, terms);
        totalLength += terms.size();
        for (String term : new java.util.HashSet<>(terms)) {
            docFrequency.merge(term, 1, Integer::sum);
        }
    }

    /** 删除文档（缺席 fail-fast）。 */
    public void remove(long docId) {
        if (!documents.containsKey(docId)) {
            throw new IllegalArgumentException("文档未索引: " + docId);
        }
        unregister(docId);
    }

    /** 内部静默卸载（add 幂等替换与 remove 共用核心）。 */
    private void unregister(long docId) {
        List<String> terms = documents.remove(docId);
        if (terms == null) {
            return;
        }
        totalLength -= terms.size();
        for (String term : new java.util.HashSet<>(terms)) {
            int remaining = docFrequency.merge(term, -1, Integer::sum);
            if (remaining <= 0) {
                docFrequency.remove(term);
            }
        }
    }

    /**
     * 按查询评分降序（并列 docId 升序；k1≥0、b∈[0,1]；
     * 无查询词命中=零分参与排序；越域 fail-fast）。
     */
    public List<Scored> rank(String query, double k1, double b) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("查询非空");
        }
        if (k1 < 0 || b < 0 || b > 1) {
            throw new IllegalArgumentException("参数越域: k1=" + k1 + " b=" + b);
        }
        List<String> queryTerms = tokenize(query);
        int docCount = documents.size();
        double avgLength = docCount == 0 ? 0 : totalLength / docCount;
        List<Scored> scored = new ArrayList<>();
        for (Map.Entry<Long, List<String>> entry : documents.entrySet()) {
            List<String> terms = entry.getValue();
            double score = 0;
            for (String queryTerm : queryTerms) {
                int tf = 0;
                for (String term : terms) {
                    if (term.equals(queryTerm)) {
                        tf++;
                    }
                }
                if (tf == 0) {
                    continue;
                }
                int df = docFrequency.getOrDefault(queryTerm, 0);
                double idf = Math.log(1 + (docCount - df + 0.5) / (df + 0.5));
                double lengthNorm = k1 * (1 - b + b * terms.size()
                        / (avgLength == 0 ? 1 : avgLength));
                score += idf * (tf * (k1 + 1)) / (tf + lengthNorm);
            }
            scored.add(new Scored(entry.getKey(), score));
        }
        scored.sort((x, y) -> {
            int byScore = Double.compare(y.score(), x.score());
            return byScore != 0 ? byScore : Long.compare(x.docId(), y.docId());
        });
        return scored;
    }

    /** 文档数读数。 */
    public int docCount() {
        return documents.size();
    }

    private List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        for (String raw : text.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!raw.isEmpty()) {
                terms.add(raw);
            }
        }
        return terms;
    }

    /** 评分条目。 */
    public record Scored(long docId, double score) {
    }
}
