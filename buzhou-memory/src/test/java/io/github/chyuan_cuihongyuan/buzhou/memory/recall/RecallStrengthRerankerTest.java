package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.memory.recall.MemoryStrengthScore.Weights;
import io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallStrengthReranker.StrengthMeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2018 / T3138：强度重排合同——同相关度新热靠前、相关度为主、
 * 权重两极退化、null 元数据降权、稳定保序、畸形 fail-fast。
 */
class RecallStrengthRerankerTest {

    private static RecallSearch.Hit hit(String content, int turn, double score) {
        return new RecallSearch.Hit(
                new BuzhouMessage(java.util.UUID.randomUUID().toString(), "s1", turn, 0,
                        Role.USER, content, java.util.List.of(), null, null, null,
                        java.util.Map.of(), java.time.Instant.now()),
                turn, score, "TEXT");
    }

    @Test
    void sameRelevanceFreshHotShouldRankFirst() {
        RecallSearch.Hit stale = hit("stale", 1, 0.8);
        RecallSearch.Hit fresh = hit("fresh", 2, 0.8);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(stale, fresh),
                h -> h.message().content().equals("fresh")
                        ? new StrengthMeta(0, 100, 1.0)      // 新热高重要
                        : new StrengthMeta(30L * 24 * 3600 * 1000, 0, 0)); // 30 天前旧冷
        assertThat(reranked.get(0).message().content()).isEqualTo("fresh"); // 强度微调同分段
    }

    @Test
    void higherRelevanceShouldStillWinDespiteLowStrength() {
        RecallSearch.Hit high = hit("high-relevance", 1, 0.95);
        RecallSearch.Hit low = hit("low-relevance", 2, 0.3);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(high, low),
                h -> h == high
                        ? new StrengthMeta(Long.MAX_VALUE / 2, 0, 0) // 高相关但零强度
                        : new StrengthMeta(0, 1000, 1.0));            // 低相关满强度
        // 0.7×0.95 = 0.665 > 0.7×0.3 + 0.3×1 = 0.51——相关度为主
        assertThat(reranked.get(0)).isSameAs(high);
    }

    @Test
    void pureStrengthModeShouldIgnoreRelevance() {
        RecallSearch.Hit lowRelevanceHot = hit("a", 1, 0.1);
        RecallSearch.Hit highRelevanceCold = hit("b", 2, 1.0);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(highRelevanceCold, lowRelevanceHot),
                h -> h == lowRelevanceHot ? new StrengthMeta(0, 100, 1.0) : null,
                null, 0.0); // relevanceWeight=0 → 纯强度序
        assertThat(reranked.get(0)).isSameAs(lowRelevanceHot);
    }

    @Test
    void pureRelevanceModeShouldKeepOriginalOrder() {
        RecallSearch.Hit first = hit("a", 1, 0.9);
        RecallSearch.Hit second = hit("b", 2, 0.5);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(first, second),
                h -> h == second ? new StrengthMeta(0, 100, 1.0) : null,
                null, 1.0); // relevanceWeight=1 → 纯相关度（强度零权重）
        assertThat(reranked).containsExactly(first, second);
    }

    @Test
    void nullMetaShouldDegradeToZeroStrength() {
        RecallSearch.Hit strong = hit("strong", 1, 0.8);
        RecallSearch.Hit noMeta = hit("no-meta", 2, 0.8);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(noMeta, strong),
                h -> h == strong ? new StrengthMeta(0, 10, 0.5) : null);
        assertThat(reranked.get(0)).isSameAs(strong); // 无元数据=零强度垫底
    }

    @Test
    void equalFusedScoreShouldKeepStableOrder() {
        RecallSearch.Hit a = hit("a", 1, 0.5);
        RecallSearch.Hit b = hit("b", 2, 0.5);
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(a, b), h -> new StrengthMeta(0, 1, 0));
        assertThat(reranked).containsExactly(a, b); // 同融合分保原序
    }

    @Test
    void timeModeHitsShouldOrderByStrength() {
        // TIME 模式 score 恒 1——重排退化为强度序
        RecallSearch.Hit t3 = hit("t3", 3, 1.0);
        RecallSearch.Hit t1 = hit("t1", 1, 1.0);
        t3 = new RecallSearch.Hit(t3.message(), t3.turn(), t3.score(), "TIME");
        t1 = new RecallSearch.Hit(t1.message(), t1.turn(), t1.score(), "TIME");
        Map<String, StrengthMeta> metas = Map.of(
                "t3", new StrengthMeta(10L * 24 * 3600 * 1000, 0, 0),
                "t1", new StrengthMeta(0, 50, 0.9));
        List<RecallSearch.Hit> reranked = RecallStrengthReranker.rerank(
                List.of(t3, t1), h -> metas.get(h.message().content()));
        assertThat(reranked.get(0).message().content()).isEqualTo("t1");
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> RecallStrengthReranker.rerank(null, h -> null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecallStrengthReranker.rerank(List.of(), h -> null,
                new Weights(1, 0, 0), 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecallStrengthReranker.rerank(List.of(), h -> null,
                new Weights(1, 0, 0), -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
