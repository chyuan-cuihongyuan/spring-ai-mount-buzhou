package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 模糊召回查询（wayfinder2 impl-15 / T41 / docs/spec/12 §memory-13，Letta
 * conversation_search 四模式）：text（子串）/ time（轮次范围倒序）/
 * embedding（向量近邻）/ hybrid（RRF 融合）——在<b>消息台账单一事实源</b>上执行
 * （无第二向量库——「单库」精神；embedding 由共享 EmbeddingProvider 提供，
 * 未注入时 EMBEDDING/HYBRID 显式降级）。
 */
public final class RecallSearch {

    /** 查询形状。 */
    public record Query(Mode mode, String text, Integer fromTurn, Integer toTurn, int limit) {
        public Query {
            limit = limit <= 0 ? 10 : limit;
        }
    }

    public enum Mode {
        TEXT,
        TIME,
        EMBEDDING,
        HYBRID
    }

    /** 命中：原文消息 + 元数据（turn / score）。 */
    public record Hit(BuzhouMessage message, int turn, double score, String mode) {
    }

    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider provider;

    public RecallSearch(io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider provider) {
        this.provider = provider;
    }

    /** 执行检索（history = 持久层原文）。 */
    public List<Hit> search(List<BuzhouMessage> history, Query query) {
        record Scored(BuzhouMessage message, int turn, double score) {
        }
        List<Scored> scored = new ArrayList<>();
        int from = query.fromTurn() == null ? 0 : query.fromTurn();
        int to = query.toTurn() == null ? Integer.MAX_VALUE : query.toTurn();
        for (BuzhouMessage message : history) {
            if (message.turnSeq() < from || message.turnSeq() > to
                    || message.content() == null || message.content().isBlank()) {
                continue;
            }
            double textScore = query.text() == null || query.text().isBlank() ? 0
                    : substringRelevance(message.content(), query.text());
            double vectorScore = provider == null || query.text() == null || query.text().isBlank()
                    ? 0
                    : io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider.cosine(
                            provider.embed(message.content()), provider.embed(query.text()));
            double score = switch (query.mode()) {
                case TEXT -> textScore;
                case TIME -> 1.0; // 时间模式不评分，按轮次倒序
                case EMBEDDING -> vectorScore;
                case HYBRID -> rrf(textScore, vectorScore);
            };
            if (query.mode() == Mode.TIME || score > 0) {
                scored.add(new Scored(message, message.turnSeq(), score));
            }
        }
        Comparator<Scored> ordering = query.mode() == Mode.TIME
                ? Comparator.comparingInt((Scored s) -> s.turn).reversed()
                : Comparator.comparingDouble((Scored s) -> s.score).reversed();
        return scored.stream().sorted(ordering)
                .limit(query.limit())
                .map(s -> new Hit(s.message, s.turn, s.score, query.mode().name()))
                .toList();
    }

    /** 向量/文本双轨是否可用（EMBEDDING/HYBRID 需 provider）。 */
    public boolean vectorReady() {
        return provider != null;
    }

    /** 简约文本相关度：命中词元占比（子串直接命中加权）。 */
    private static double substringRelevance(String content, String query) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        if (lowerContent.contains(lowerQuery)) {
            return 1.0;
        }
        String[] tokens = lowerQuery.split("[^\\p{L}\\p{N}]+");
        int hit = 0;
        int total = 0;
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            total++;
            if (lowerContent.contains(token)) {
                hit++;
            }
        }
        return total == 0 ? 0 : (double) hit / total;
    }

    /** Reciprocal Rank Fusion 近似（分数域融合；rank 域 RRF 的简化）。 */
    private static double rrf(double textScore, double vectorScore) {
        return 0.6 * vectorScore + 0.4 * textScore;
    }
}
