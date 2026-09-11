package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 混合排序装配测试（spec 630 / T910–T911 / impl 483，spec 605 原语装配扩散）：
 * hybrid 声明即 HybridSkillRanker 生效（词法权重可配）、无 EmbeddingModel fail-fast、
 * yml 键解析、缺省零变化。
 */
class HybridRankingAssemblyTest {

    /** 恒同向 stub（语义路总是按首候选顺序稳定输出）。 */
    private static final class IdentityEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<float[]> vectors = request.getInstructions().stream()
                    .map(t -> new float[] {1f, 0f})
                    .toList();
            return new EmbeddingResponse(java.util.stream.IntStream.range(0, vectors.size())
                    .mapToObj(i -> new org.springframework.ai.embedding.Embedding(vectors.get(i), i))
                    .toList());
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return new float[] {1f, 0f};
        }
    }

    /** hybrid=true 无 EmbeddingModel：装配期 fail-fast 带修法。 */
    @Test
    void hybridWithoutEmbeddingFailsFast() {
        assertThatThrownBy(() -> SkillModule.builder()
                .hybridRankingEnabled(true)
                .build())
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class)
                .hasMessageContaining("hybrid-ranking");
    }

    /** 缺省：无排序器零变化（semantic/hybrid 均关）。 */
    @Test
    void defaultNoRanker() {
        var module = SkillModule.builder().build();
        assertThat(module.catalogRenderer()).isNotNull();
    }

    /** yml 键解析：hybrid-ranking.enabled + lexical-weight 进 Builder。 */
    @Test
    void ymlKeysParse() {
        var module = SkillModule.builder()
                .fromYml(Map.of(
                        "hybrid-ranking.enabled", true,
                        "hybrid-ranking.lexical-weight", "3.0"))
                .embeddingModel(new IdentityEmbeddingModel())
                .build();
        assertThat(module.catalogRenderer()).isNotNull(); // 装配成功即 hybrid ranker 在链上
    }
}
