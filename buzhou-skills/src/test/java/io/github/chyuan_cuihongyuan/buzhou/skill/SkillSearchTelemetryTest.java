package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 116 §B / T414：skill_search 遥测红队——hit / miss / miss-semantic 三态
 * 计数（命中率即技能可发现性信号：高 miss-semantic 率 = 命名/描述与问法脱节）。
 */
class SkillSearchTelemetryTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + ":" + String.join("=", tagKeyValue));
        }

        @Override
        public void timer(String name, java.time.Duration duration, String... tagKeyValue) {
        }
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    private static SkillRegistry registryOf(SkillMetadata... catalog) {
        return new SkillRegistry() {
            @Override
            public List<SkillMetadata> listFor(String appId, String agentName) {
                return List.of(catalog);
            }

            @Override
            public List<SkillMetadata> listAllFor(String appId, String agentName) {
                return List.of(catalog);
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

    private static SkillMetadata meta(String name, String desc) {
        return new SkillMetadata(name, desc, List.of(), SkillSource.CLASSPATH);
    }

    @Test
    void hitMissAndMissSemanticEachCounted() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        var embedder = new SemanticRankingTest.StubEmbeddingModel(t -> new float[]{1f, 0f});
        SkillSearchTool tool = new SkillSearchTool(
                registryOf(meta("deploy", "部署发布流水线")), null,
                new SemanticSkillRanker(embedder));

        String hit = tool.call("{\"query\":\"deploy\"}", null);
        String missSemantic = tool.call("{\"query\":\"怎么发版上线\"}", null); // 零子串 → 语义提示

        assertThat(hit).contains("deploy");
        assertThat(missSemantic).contains("语义最近");
        assertThat(metrics.counters).containsExactlyInAnyOrder(
                "buzhou.skills.search:outcome=hit",
                "buzhou.skills.search:outcome=miss-semantic");
    }

    @Test
    void pureMissWithoutRankerCountsMiss() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        SkillSearchTool tool = new SkillSearchTool(
                registryOf(meta("deploy", "部署发布流水线")), null); // 无 ranker

        String miss = tool.call("{\"query\":\"数据库\"}", null);

        assertThat(miss).contains("无匹配技能");
        assertThat(metrics.counters).containsExactly(
                "buzhou.skills.search:outcome=miss");
    }
}
