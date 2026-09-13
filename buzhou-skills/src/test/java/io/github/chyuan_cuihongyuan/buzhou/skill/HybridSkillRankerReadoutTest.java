package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 744 / T1090–T1091：混合排序融合权重读数——声明生效确认面（638 同型）、
 * 融合计数、降级不计融合。
 */
class HybridSkillRankerReadoutTest {

    /** 向量 stub：query/skill 文本各有稳定向量（语义路不 bypass）。 */
    static final class StubEmbeddingModel implements EmbeddingModel {
        final Function<String, float[]> mapping;

        StubEmbeddingModel(Function<String, float[]> mapping) {
            this.mapping = mapping;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> texts = request.getInstructions();
            List<org.springframework.ai.embedding.Embedding> results = new java.util.ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                results.add(new org.springframework.ai.embedding.Embedding(
                        mapping.apply(texts.get(i)), i));
            }
            return new EmbeddingResponse(results);
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return mapping.apply(document.getText());
        }
    }

    private static SkillMetadata skill(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

    @Test
    void weightsAndFusedCountAreExposed() {
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("deploy app", new float[]{1f, 0f});
        vectors.put("deploy 技能，处理发布流程", new float[]{0.9f, 0.1f});
        vectors.put("audit 技能，审计日志", new float[]{0.0f, 1f});
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);

        HybridSkillRanker ranker = new HybridSkillRanker(
                new SemanticSkillRanker(embedder), new LexicalSkillRanker(), 2.0, 1.0);
        assertThat(ranker.semanticWeight()).isEqualTo(2.0);
        assertThat(ranker.lexicalWeight()).isEqualTo(1.0);

        List<SkillMetadata> candidates = List.of(
                skill("deploy", "deploy 技能，处理发布流程"),
                skill("audit", "audit 技能，审计日志"));
        long before = ranker.fusedCount();
        ranker.rank(candidates, "deploy app");
        assertThat(ranker.fusedCount()).isEqualTo(before + 1); // 两路齐备完成融合
    }
}
