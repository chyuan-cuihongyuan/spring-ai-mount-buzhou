package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 倒排索引（spec 7019 / U7239 / impl 2271）——Lucene/Elasticsearch
 * 基础面思想：**词项 → 有序 posting 列表**——布尔 AND=列表
 * 交集（沿有序双指针归并）、OR=并集——每查询全文档扫列
 * O(N·L)（语料放大）的病解（词典命中后只走 posting）。
 * 分词：非字母数字小写归一切分（确定性词法）；posting 升序
 * ——同语料同结果完全确定。重复索引同 docId 幂等（先删后
 * 插语义）。
 *
 * <p>与 MinHashSketch（同包）同族不同面：精确词项布尔召回
 * vs 近似相似度签名。
 */
public final class InvertedIndex {

    private final Map<String, TreeSet<Long>> postings = new TreeMap<>();
    private final Map<Long, TreeSet<String>> docTerms = new TreeMap<>();

    /** 索引文档（重复 docId 幂等替换；null/空白文本 fail-fast）。 */
    public void add(long docId, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本非空");
        }
        unregister(docId);
        TreeSet<String> terms = new TreeSet<>();
        for (String raw : text.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!raw.isEmpty()) {
                terms.add(raw);
            }
        }
        docTerms.put(docId, terms);
        for (String term : terms) {
            postings.computeIfAbsent(term, k -> new TreeSet<>()).add(docId);
        }
    }

    /** 删除文档（缺席 fail-fast——语义与统计一致性绑定）。 */
    public void remove(long docId) {
        if (!docTerms.containsKey(docId)) {
            throw new IllegalArgumentException("文档未索引: " + docId);
        }
        unregister(docId);
    }

    /** 内部静默卸载（add 幂等替换与 remove 共用核心）。 */
    private void unregister(long docId) {
        TreeSet<String> terms = docTerms.remove(docId);
        if (terms == null) {
            return;
        }
        for (String term : terms) {
            TreeSet<Long> docs = postings.get(term);
            docs.remove(docId);
            if (docs.isEmpty()) {
                postings.remove(term);
            }
        }
    }

    /** AND 检索（升序 docId；空词表=无约束——fail-fast 更诚实）。 */
    public List<Long> searchAnd(String... terms) {
        List<TreeSet<Long>> lists = postingLists(terms);
        if (lists.isEmpty()) {
            return List.of();
        }
        TreeSet<Long> result = new TreeSet<>(lists.get(0));
        for (int i = 1; i < lists.size(); i++) {
            result.retainAll(lists.get(i));
        }
        return new ArrayList<>(result);
    }

    /** OR 检索（升序 docId）。 */
    public List<Long> searchOr(String... terms) {
        TreeSet<Long> result = new TreeSet<>();
        for (TreeSet<Long> docs : postingLists(terms)) {
            result.addAll(docs);
        }
        return new ArrayList<>(result);
    }

    /** 词项文档频次读数（缺席 0）。 */
    public int documentFrequency(String term) {
        requireTerm(term);
        TreeSet<Long> docs = postings.get(normalize(term));
        return docs == null ? 0 : docs.size();
    }

    /** 词典规模读数。 */
    public int termCount() {
        return postings.size();
    }

    /** 已索引文档数读数。 */
    public int docCount() {
        return docTerms.size();
    }

    private List<TreeSet<Long>> postingLists(String... terms) {
        if (terms == null || terms.length == 0) {
            throw new IllegalArgumentException("至少一个词项");
        }
        List<TreeSet<Long>> lists = new ArrayList<>();
        for (String term : terms) {
            requireTerm(term);
            TreeSet<Long> docs = postings.get(normalize(term));
            if (docs != null) {
                lists.add(docs);
            }
        }
        return lists;
    }

    private String normalize(String term) {
        return term.toLowerCase(java.util.Locale.ROOT).trim();
    }

    private void requireTerm(String term) {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("词项非空");
        }
    }
}
