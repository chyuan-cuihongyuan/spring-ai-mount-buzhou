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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 混合技能排序测试（spec 605 / T860–T861 / impl 458）：BM25 词法命中、CJK bigram、
 * RRF 融合次序、语义降级纯词法、tokenize 边界、构造校验。
 */
class HybridSkillRankerTest {

    /** stub 嵌入模型（同 SemanticRankingTest 形态）。 */
    static final class StubEmbeddingModel implements EmbeddingModel {
        final Function<String, float[]> mapping;

        StubEmbeddingModel(Function<String, float[]> mapping) {
            this.mapping = mapping;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> texts = request.getInstructions();
            float[][] vectors = texts.stream().map(t -> {
                float[] v = mapping.apply(t);
                if (v == null) {
                    throw new IllegalStateException("stub embed failure: " + t);
                }
                return v;
            }).toArray(float[][]::new);
            return new EmbeddingResponse(java.util.stream.IntStream.range(0, vectors.length)
                    .mapToObj(i -> new org.springframework.ai.embedding.Embedding(vectors[i], i))
                    .toList());
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return mapping.apply(document.getText());
        }
    }

    private static SkillMetadata skill(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

    /** BM25 词法命中：精确词（错误码）文档排到无命中文档之前。 */
    @Test
    void lexicalRankPrefersExactTermHits() {
        LexicalSkillRanker ranker = new LexicalSkillRanker();
        List<SkillMetadata> ranked = ranker.rank(List.of(
                skill("deploy", "部署技能，处理发布流程"),
                skill("errcode", "E429 错误码处理与退避说明"),
                skill("audit", "审计轮换")), "怎么处理 E429");
        assertThat(ranked.get(0).name()).isEqualTo("errcode");
    }

    /** CJK bigram：中文词命中生效且不跨非汉字边界。 */
    @Test
    void cjkBigramMatchesChineseDescriptions() {
        LexicalSkillRanker ranker = new LexicalSkillRanker();
        List<SkillMetadata> ranked = ranker.rank(List.of(
                skill("deploy", "部署技能"),
                skill("audit", "审计轮换与密钥管理")), "审计轮换怎么做");
        assertThat(ranked.get(0).name()).isEqualTo("audit");
    }

    /** tokenize 边界：ASCII 小写化、CJK 分段不跨界、空串安全。 */
    @Test
    void tokenizeBoundaries() {
        assertThat(LexicalSkillRanker.tokenize("Deploy E429")).containsExactly("deploy", "e429");
        assertThat(LexicalSkillRanker.tokenize("审计 rotation 轮换"))
                .containsExactlyInAnyOrder("审计", "rotation", "轮换"); // 袋词序无关（BM25 只看计数）
        assertThat(LexicalSkillRanker.tokenize("")).isEmpty();
        assertThat(LexicalSkillRanker.tokenize(null)).isEmpty();
    }

    /** RRF 融合：语义序 [a,b,c]、词法序 [b,a,c]——等权并列保原序（a 前），词法 3:1 加权 b 胜。 */
    @Test
    void rrfFusesTwoOrderings() {
        // 嵌入文本 = name+"\n"+description：a 与查询同向（语义第 1）；b 略偏；c 正交
        StubEmbeddingModel embedder = new StubEmbeddingModel(t -> {
            if (t.startsWith("a\n")) {
                return new float[] {1f, 0f};
            }
            if (t.startsWith("b\n")) {
                return new float[] {0.95f, 0.1f};
            }
            if (t.startsWith("keyword")) {
                return new float[] {1f, 0f}; // 查询文本本身
            }
            return new float[] {0f, 1f};
        });
        List<SkillMetadata> candidates = List.of(
                skill("a", "alpha 语义描述"),
                skill("b", "keyword keyword keyword 词法描述"),
                skill("c", "其他"));
        HybridSkillRanker hybrid = new HybridSkillRanker(
                new SemanticSkillRanker(embedder), new LexicalSkillRanker());
        List<SkillMetadata> ranked = hybrid.rank(candidates, "keyword alpha");
        // 语义序 [a,b,c]；词法：b 命中 keyword×3（tf 饱和）> a 命中 alpha×1 → [b,a,c]
        // 等权 RRF：a = 1/61+1/62 = b → 并列保原序（a 前）
        assertThat(ranked.get(0).name()).isEqualTo("a");

        HybridSkillRanker lexicalHeavy = new HybridSkillRanker(
                new SemanticSkillRanker(embedder), new LexicalSkillRanker(), 1.0, 3.0);
        // b = 1/61+3/62 > a = 1/61+3/63 → 词法加权胜
        assertThat(lexicalHeavy.rank(candidates, "keyword alpha").get(0).name()).isEqualTo("b");
    }

    /** 语义路嵌入失败 → 纯词法序 + semanticFallbackCount 观测。 */
    @Test
    void semanticFailureFallsBackToLexical() {
        StubEmbeddingModel failing = new StubEmbeddingModel(t -> null);
        HybridSkillRanker hybrid = new HybridSkillRanker(
                new SemanticSkillRanker(failing), new LexicalSkillRanker());
        List<SkillMetadata> ranked = hybrid.rank(List.of(
                skill("deploy", "部署技能"),
                skill("errcode", "E429 错误码处理")), "E429");
        assertThat(ranked.get(0).name()).isEqualTo("errcode");
        assertThat(hybrid.semanticFallbackCount()).isEqualTo(1);
    }

    /** 无效 hint / 单候选：原样返回；构造校验。 */
    @Test
    void guardsAndValidation() {
        HybridSkillRanker hybrid = new HybridSkillRanker(
                new SemanticSkillRanker(new StubEmbeddingModel(t -> new float[] {1f})),
                new LexicalSkillRanker());
        List<SkillMetadata> single = List.of(skill("a", "d"));
        assertThat(hybrid.rank(single, "hint")).isSameAs(single);
        assertThat(hybrid.rank(List.of(skill("a", "d"), skill("b", "d")), "  ")).hasSize(2);

        assertThatThrownBy(() -> new HybridSkillRanker(null, new LexicalSkillRanker()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HybridSkillRanker(
                new SemanticSkillRanker(new StubEmbeddingModel(t -> new float[] {1f})),
                new LexicalSkillRanker(), 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
