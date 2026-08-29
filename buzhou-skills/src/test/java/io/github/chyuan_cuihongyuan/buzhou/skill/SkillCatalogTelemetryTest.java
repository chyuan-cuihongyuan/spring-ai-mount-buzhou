package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 110 §B / T402：技能目录注入遥测红队——injected 每注入 +1；overflow 两值
 * tag（truncated 截断 / fit 全量）；空目录零计数。截断频率是 catalog-max-entries
 * 调优的信号（过高截断率 = 预算过小）。
 */
class SkillCatalogTelemetryTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + (tagKeyValue.length == 0 ? ""
                    : ":" + String.join("=", tagKeyValue)));
        }

        @Override
        public void timer(String name, java.time.Duration duration, String... tagKeyValue) {
        }
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    private static SkillMetadata meta(String name) {
        return new SkillMetadata(name, "desc-" + name, List.of(), SkillSource.CLASSPATH);
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

    @Test
    void injectionCountsTruncatedAndFitOutcomes() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");

        // 截断：排序路径预算 2 截 3 技能（固定向量 stub——排序无碍截断判定）
        var embedder = new SemanticRankingTest.StubEmbeddingModel(t -> new float[]{1f, 0f});
        SkillCatalogRendererImpl truncated = new SkillCatalogRendererImpl(index,
                registryOf(List.of(meta("a"), meta("b"), meta("c"))),
                new SemanticSkillRanker(embedder), 2);
        // 全量：预算 64 放 2 技能
        SkillCatalogRendererImpl fit = new SkillCatalogRendererImpl(index,
                registryOf(List.of(meta("x"), meta("y"))), null, 64);

        truncated.renderCatalog("s1", "任意问法");
        fit.renderCatalog("s1");

        assertThat(metrics.counters).containsExactlyInAnyOrder(
                "buzhou.skills.catalog-injected",
                "buzhou.skills.catalog-overflow:outcome=truncated",
                "buzhou.skills.catalog-injected",
                "buzhou.skills.catalog-overflow:outcome=fit");
    }

    @Test
    void emptyCatalogEmitsNothing() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s-empty", "app", "agent");

        SkillCatalogRendererImpl renderer = new SkillCatalogRendererImpl(index,
                registryOf(List.of()));

        assertThat(renderer.renderCatalog("s-empty")).isEmpty();
        assertThat(metrics.counters).isEmpty(); // 空目录零注入零计数
    }
}
