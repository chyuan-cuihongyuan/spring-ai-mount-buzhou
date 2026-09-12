package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能目录词法排序器（spec 605 / T860，weaviate/Qdrant hybrid search 借鉴；
 * opt-in，默认关闭）：候选技能按「name + description」对问法做 BM25 词法打分
 * 降序——精确词命中（型号/错误码/专有名词）场景补语义排序的判别盲区。
 *
 * <p><b>分词</b>：ASCII 词（{@code [a-zA-Z0-9]+} 小写化）+ CJK bigram（相邻汉字对，
 * 单汉字短串退化为 unigram）——中文描述无空格分隔的务实词法口径。
 *
 * <p><b>诚实边界</b>：BM25 参数 k1/b 取经典值（1.2 / 0.75）不可配——词法面只是
 * 混合排序的一路输入，单独调参收益低于复杂度；问法 null/空或无有效 token → 原序。
 */
public final class LexicalSkillRanker {

    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;
    private static final Pattern ASCII_TOKEN = Pattern.compile("[a-zA-Z0-9]+");
    private static final Pattern CJK_CHAR = Pattern.compile("[\\u4e00-\\u9fff]");

    /** 排序（BM25 降序、并列保原序稳定）；hint 无有效 token → 原样返回。 */
    public List<SkillMetadata> rank(List<SkillMetadata> candidates, String queryHint) {
        if (candidates == null || candidates.size() <= 1
                || queryHint == null || queryHint.isBlank()) {
            return candidates;
        }
        List<String> queryTokens = tokenize(queryHint);
        if (queryTokens.isEmpty()) {
            return candidates;
        }
        List<Map<String, Integer>> docTokenCounts = new ArrayList<>(candidates.size());
        List<Integer> docLengths = new ArrayList<>(candidates.size());
        for (SkillMetadata meta : candidates) {
            List<String> tokens = tokenize(meta.name() + "\n"
                    + (meta.description() == null ? "" : meta.description()));
            Map<String, Integer> counts = new HashMap<>();
            tokens.forEach(t -> counts.merge(t, 1, Integer::sum));
            docTokenCounts.add(counts);
            docLengths.add(Math.max(1, tokens.size()));
        }
        double avgDl = docLengths.stream().mapToInt(Integer::intValue).average().orElse(1);

        // 查询词的候选集级文档频率（含该词的文档数）——BM25 IDF 口径
        Map<String, Integer> dfByToken = new HashMap<>();
        for (String token : queryTokens) {
            int df = 0;
            for (Map<String, Integer> counts : docTokenCounts) {
                if (counts.containsKey(token)) {
                    df++;
                }
            }
            dfByToken.put(token, df);
        }

        record Scored(int index, double score) {
        }
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            scored.add(new Scored(i, bm25(queryTokens, dfByToken, docTokenCounts.get(i),
                    docLengths.get(i), avgDl, candidates.size())));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparingInt(Scored::index));
        List<SkillMetadata> ranked = new ArrayList<>(candidates.size());
        for (Scored s : scored) {
            ranked.add(candidates.get(s.index()));
        }
        return ranked;
    }

    /** BM25(q, d)：Σ IDF(t) × tf×(k1+1) / (tf + k1×(1−b+b×dl/avgdl))；IDF = ln(1+(N−df+0.5)/(df+0.5))。 */
    private static double bm25(List<String> queryTokens, Map<String, Integer> dfByToken,
            Map<String, Integer> docCounts, int docLength, double avgDl, int totalDocs) {
        double score = 0;
        for (String token : queryTokens) {
            int tf = docCounts.getOrDefault(token, 0);
            if (tf == 0) {
                continue;
            }
            double idf = Math.log(1 + (totalDocs - dfByToken.getOrDefault(token, 1) + 0.5) / 1.5);
            double norm = BM25_K1 * (1 - BM25_B + BM25_B * docLength / avgDl);
            score += idf * (tf * (BM25_K1 + 1)) / (tf + norm);
        }
        return score;
    }

    /** 分词：ASCII 词小写化 + CJK 连续段 bigram（单字串退化 unigram，段间不跨界）。 */
    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Matcher ascii = ASCII_TOKEN.matcher(text);
        while (ascii.find()) {
            tokens.add(ascii.group().toLowerCase());
        }
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (CJK_CHAR.matcher(String.valueOf(c)).matches()) {
                run.append(c);
            } else if (!run.isEmpty()) {
                appendCjkTokens(tokens, run.toString());
                run.setLength(0);
            }
        }
        if (!run.isEmpty()) {
            appendCjkTokens(tokens, run.toString());
        }
        return tokens;
    }

    private static void appendCjkTokens(List<String> tokens, String segment) {
        if (segment.length() == 1) {
            tokens.add(segment);
        } else {
            for (int i = 0; i + 1 < segment.length(); i++) {
                tokens.add(segment.substring(i, i + 2));
            }
        }
    }
}
