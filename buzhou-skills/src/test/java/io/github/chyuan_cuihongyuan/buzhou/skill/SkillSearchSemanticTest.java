package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * skill_search 语义面红队（spec 73 §B / T298）：命中集按 query 相似度排序（相关在前）；
 * 零子串命中给语义最近 3 条提示；无 ranker 行为与历史一致（注册序 + 空命中旧文案）。
 */
class SkillSearchSemanticTest {

    static final class StubEmbeddingModel implements EmbeddingModel {
        final Map<String, float[]> vectors = new HashMap<>();

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> texts = request.getInstructions();
            float[][] vs = texts.stream()
                    .map(t -> vectors.computeIfAbsent(t, k -> new float[]{0.01f, 1f})) // 未见文本给正交兜底
                    .toArray(float[][]::new);
            return new EmbeddingResponse(java.util.stream.IntStream.range(0, vs.length)
                    .mapToObj(i -> new org.springframework.ai.embedding.Embedding(vs[i], i))
                    .toList());
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return vectors.computeIfAbsent(document.getText(), k -> new float[]{0.01f, 1f});
        }
    }

    private static SkillRegistry registryOf(List<SkillMetadata> catalog) {
        return new SkillRegistry() {
            @Override
            public List<SkillMetadata> listFor(String appId, String agentName) {
                return catalog;
            }

            @Override
            public java.util.Optional<Skill> load(String appId, String agentName, String name) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<String> loadResource(String appId, String agentName,
                    String skillName, String relativePath) {
                return java.util.Optional.empty();
            }
        };
    }

    private static SkillMetadata meta(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

    @Test
    void hitsRankedBySemanticRelevance() {
        StubEmbeddingModel embedder = new StubEmbeddingModel();
        embedder.vectors.put("sql", new float[]{1f, 0f}); // 检索 query 向量
        embedder.vectors.put("慢查询", new float[]{1f, 0f});
        embedder.vectors.put("sql-tuning\n慢 SQL 诊断", new float[]{1f, 0f});
        embedder.vectors.put("sql-backup\nSQL 备份", new float[]{0f, 1f});
        List<SkillMetadata> catalog = List.of(
                meta("sql-backup", "SQL 备份"),
                meta("sql-tuning", "慢 SQL 诊断"));
        SkillSearchTool tool = new SkillSearchTool(registryOf(catalog), new SessionBindingIndex(),
                new SemanticSkillRanker(embedder));

        String out = tool.call("{\"query\":\"sql\"}", null);

        assertThat(out).contains("sql-backup").contains("sql-tuning"); // 子串都命中
        assertThat(out.indexOf("sql-tuning")).isLessThan(out.indexOf("sql-backup")); // 语义相关在前
    }

    @Test
    void zeroHitsGiveSemanticSuggestions() {
        StubEmbeddingModel embedder = new StubEmbeddingModel();
        embedder.vectors.put("部署流水线", new float[]{1f, 0f});
        embedder.vectors.put("deploy\n部署发布流水线", new float[]{1f, 0f});
        embedder.vectors.put("logs\n日志清理", new float[]{0f, 1f});
        List<SkillMetadata> catalog = List.of(
                meta("deploy", "部署发布流水线"),
                meta("logs", "日志清理"));
        SkillSearchTool tool = new SkillSearchTool(registryOf(catalog), new SessionBindingIndex(),
                new SemanticSkillRanker(embedder));

        String out = tool.call("{\"query\":\"部署流水线\"}", null); // 无子串命中（清单无此串）

        assertThat(out).contains("语义最近").contains("deploy"); // 近邻提示
    }

    @Test
    void withoutRankerBehaviorUnchanged() {
        List<SkillMetadata> catalog = List.of(meta("sql-backup", "SQL 备份"), meta("a", "b"));
        SkillSearchTool tool = new SkillSearchTool(registryOf(catalog), new SessionBindingIndex());

        String hit = tool.call("{\"query\":\"sql\"}", null);
        assertThat(hit).contains("sql-backup").contains("用 load_skill");
        assertThat(hit.indexOf("匹配技能")).isZero();

        String miss = tool.call("{\"query\":\"zzz\"}", null);
        assertThat(miss).contains("无匹配技能"); // 旧文案（无近邻提示）
    }
}
