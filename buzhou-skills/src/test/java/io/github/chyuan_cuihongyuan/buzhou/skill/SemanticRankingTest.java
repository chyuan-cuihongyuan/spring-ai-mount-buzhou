package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 技能目录语义排序红队（spec 59 §B / T267）：预算内保最相关（stub 向量手工控相似度）；
 * 嵌入失败回退原序 + bypass 计数；无问法原序；禁用零变化；enabled 无 bean fail-fast；
 * 向量缓存零重复嵌入；64 技能缓存命中排序耗时上界（perf 哨兵并入——进程内 stub 无
 * 容器依赖，收紧为普通断言）。
 */
class SemanticRankingTest {

    /** stub 嵌入模型：文本 → 向量映射（缺失文本抛异常模拟嵌入故障）。 */
    static final class StubEmbeddingModel implements EmbeddingModel {
        final Function<String, float[]> mapping;
        final Map<String, Integer> calls = new HashMap<>();

        StubEmbeddingModel(Function<String, float[]> mapping) {
            this.mapping = mapping;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> texts = request.getInstructions();
            float[][] vectors = texts.stream().map(t -> {
                calls.merge(t, 1, Integer::sum);
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

    /** 固定清单的假 registry（注册序 = 声明序）。 */
    private static SkillRegistry registryOf(List<SkillMetadata> catalog) {
        return new SkillRegistry() {
            @Override
            public List<SkillMetadata> listFor(String appId, String agentName) {
                return catalog;
            }

            @Override
            public Optional<Skill> load(String appId, String agentName, String name) {
                return Optional.empty();
            }

            @Override
            public Optional<String> loadResource(String appId, String agentName,
                    String skillName, String relativePath) {
                return Optional.empty();
            }
        };
    }

    private static SkillMetadata meta(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

    private static SessionBindingIndex boundIndex() {
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");
        return index;
    }

    /** 预算 2 截 3 技能：与问法最相关的排前进预算，无关技能被截（溢出提示口径不变）。 */
    @Test
    void relevantSkillSurvivesBudgetCut() {
        // 向量空间：问法与 skill-c 同向（cos=1），与 a/b 正交（cos=0）
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("帮我诊断慢 SQL", new float[]{1f, 0f});
        vectors.put("sql-tuning\n慢 SQL 诊断与索引优化", new float[]{1f, 0f});
        vectors.put("code-review\n代码评审清单", new float[]{0f, 1f});
        vectors.put("deploy\n部署发布流水线", new float[]{0f, 1f});
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);

        List<SkillMetadata> catalog = List.of(
                meta("code-review", "代码评审清单"),
                meta("deploy", "部署发布流水线"),
                meta("sql-tuning", "慢 SQL 诊断与索引优化"));
        SkillCatalogRendererImpl renderer = new SkillCatalogRendererImpl(boundIndex(),
                registryOf(catalog), new SemanticSkillRanker(embedder), 2);

        String out = renderer.renderCatalog("s1", "帮我诊断慢 SQL").orElseThrow();
        assertThat(out).contains("sql-tuning");          // 最相关进预算
        assertThat(out.indexOf("sql-tuning")).isLessThan(out.indexOf("code-review")); // 排最前
        assertThat(out).contains("另有 1 个技能因目录注入上限未列出"); // 溢出口径不变
        assertThat(out).doesNotContain("deploy");        // 无关者被截（a/b 并列保原序）
    }

    /** 嵌入失败 → 整体回退注册序 + bypass 计数（注入链路不断）。 */
    @Test
    void embedFailureFallsBackToRegistrationOrder() {
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("任一问法", new float[]{1f, 0f});
        // 技能文本无向量 → embed 抛异常
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        SemanticSkillRanker ranker = new SemanticSkillRanker(embedder);

        List<SkillMetadata> catalog = List.of(meta("a", "desc-a"), meta("b", "desc-b"));
        List<SkillMetadata> ranked = ranker.rank(catalog, "任一问法");

        assertThat(ranked).containsExactlyElementsOf(catalog); // 原序
        assertThat(ranker.bypassCount()).isEqualTo(1);
    }

    /** 无问法（null/空）→ 原序；禁用（ranker=null）→ 与无 hint 输出完全一致。 */
    @Test
    void noHintOrDisabledKeepsOriginalBehavior() {
        List<SkillMetadata> catalog = List.of(meta("a", "d1"), meta("b", "d2"), meta("c", "d3"));
        StubEmbeddingModel embedder = new StubEmbeddingModel(t -> new float[]{1f, 0f});
        SkillCatalogRendererImpl ranked = new SkillCatalogRendererImpl(boundIndex(),
                registryOf(catalog), new SemanticSkillRanker(embedder), 64);
        SkillCatalogRendererImpl plain = new SkillCatalogRendererImpl(boundIndex(),
                registryOf(catalog));

        assertThat(ranked.renderCatalog("s1", null)).isEqualTo(plain.renderCatalog("s1"));
        assertThat(ranked.renderCatalog("s1", "  ")).isEqualTo(plain.renderCatalog("s1"));
        assertThat(ranked.renderCatalog("s1")).isEqualTo(plain.renderCatalog("s1"));
    }

    /** enabled=true 而无 EmbeddingModel → build() fail-fast 带修法（不静默失效）。 */
    @Test
    void enabledWithoutEmbeddingModelFailsFastWithFix() {
        assertThatThrownBy(() -> SkillModule.builder()
                .fromYml(Map.of("semantic-ranking.enabled", true))
                .scanLocations(List.of())
                .build())
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("semantic-ranking")
                .hasMessageContaining("EmbeddingModel");
    }

    /** 向量缓存：同技能集两轮排序，技能文本只嵌一次（问法每轮一次）。 */
    @Test
    void skillVectorsCachedAcrossRanks() {
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("q1", new float[]{1f, 0f});
        vectors.put("q2", new float[]{0f, 1f});
        vectors.put("a\nskill-a", new float[]{1f, 0f});
        vectors.put("b\nskill-b", new float[]{0f, 1f});
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        SemanticSkillRanker ranker = new SemanticSkillRanker(embedder);

        List<SkillMetadata> catalog = List.of(meta("a", "skill-a"), meta("b", "skill-b"));
        ranker.rank(catalog, "q1");
        ranker.rank(catalog, "q2");

        assertThat(embedder.calls.getOrDefault("a\nskill-a", 0)).isEqualTo(1); // 技能向量缓存
        assertThat(embedder.calls.getOrDefault("b\nskill-b", 0)).isEqualTo(1);
        assertThat(embedder.calls.getOrDefault("q1", 0)).isEqualTo(1);         // 问法各一轮
        assertThat(embedder.calls.getOrDefault("q2", 0)).isEqualTo(1);
    }

    /** 64 技能缓存命中排序耗时上界（进程内 stub；越界 = 量级回归信号）。 */
    @Test
    void sixtyFourSkillsCachedRankWithinBudget() {
        Map<String, float[]> vectors = new HashMap<>();
        List<SkillMetadata> catalog = new java.util.ArrayList<>();
        for (int i = 0; i < 64; i++) {
            String name = "skill-" + i;
            vectors.put(name + "\ndesc-" + i, new float[]{i % 8, (i + 3) % 8, 1f});
            catalog.add(meta(name, "desc-" + i));
        }
        vectors.put("warm-query", new float[]{1f, 1f, 1f});
        StubEmbeddingModel embedder = new StubEmbeddingModel(vectors::get);
        SemanticSkillRanker ranker = new SemanticSkillRanker(embedder);
        ranker.rank(catalog, "warm-query"); // 预热缓存

        long start = System.nanoTime();
        List<SkillMetadata> ranked = ranker.rank(catalog, "warm-query");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(ranked).hasSize(64);
        assertThat(elapsedMs).as("64 技能缓存命中排序（纯内存 cosine）").isLessThan(100);
    }
}
