package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1136 / impl 875：SemanticSkillRanker 排序分布——正常排序（rankCalls）、
 * 跳过短路径（skippedTrivial）、降级（bypassed）、resetForTest 归零。
 * 桩 EmbeddingModel 同 SemanticSkillRanker 既有测试。
 */
class SemanticRankerDistTest {

    static final class StubEmbeddingModel implements org.springframework.ai.embedding.EmbeddingModel {
        final Map<String, float[]> vectors = new java.util.HashMap<>();

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(
                org.springframework.ai.embedding.EmbeddingRequest request) {
            var texts = request.getInstructions();
            float[][] vs = texts.stream()
                    .map(t -> vectors.computeIfAbsent(t, k -> new float[]{0.01f, 1f}))
                    .toArray(float[][]::new);
            return new org.springframework.ai.embedding.EmbeddingResponse(
                    java.util.stream.IntStream.range(0, vs.length)
                            .mapToObj(i -> new org.springframework.ai.embedding.Embedding(vs[i], i))
                            .toList());
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return vectors.computeIfAbsent(document.getText(), k -> new float[]{0.01f, 1f});
        }
    }

    private SemanticSkillRanker ranker;

    @BeforeEach
    void setUp() {
        SemanticSkillRanker.resetDistForTest();
        ranker = new SemanticSkillRanker(new StubEmbeddingModel());
    }

    private List<SkillMetadata> candidates(String... names) {
        return java.util.Arrays.stream(names)
                .map(n -> new SkillMetadata(n, "desc of " + n, null, null))
                .toList();
    }

    @Test
    void successfulRankCountsSucceeded() {
        var out = ranker.rank(candidates("a", "b", "c"), "query");
        assertThat(out).hasSize(3);

        SemanticSkillRanker.RankerDistStats stats = SemanticSkillRanker.distStats();
        assertThat(stats.rankCalls()).isEqualTo(1);
        assertThat(stats.succeeded()).isEqualTo(1);
        assertThat(stats.skippedTrivial()).isZero();
    }

    @Test
    void trivialInputCountsSkipped() {
        ranker.rank(candidates("a"), "query"); // 单候选
        ranker.rank(candidates("a", "b"), null); // null hint

        SemanticSkillRanker.RankerDistStats stats = SemanticSkillRanker.distStats();
        assertThat(stats.rankCalls()).isEqualTo(2);
        assertThat(stats.skippedTrivial()).isEqualTo(2);
    }

    @Test
    void resetForTestZeroesCounters() {
        ranker.rank(candidates("a", "b"), "query");
        assertThat(SemanticSkillRanker.distStats().rankCalls()).isEqualTo(1);

        SemanticSkillRanker.resetDistForTest();

        assertThat(SemanticSkillRanker.distStats().rankCalls()).isZero();
    }
}
