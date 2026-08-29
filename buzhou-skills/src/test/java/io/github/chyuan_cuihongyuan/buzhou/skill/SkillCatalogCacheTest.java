package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 168 §B / T521：目录渲染缓存红队——同目录二连击（1 miss + 1 hit，输出
 * 等值）；目录内容变（描述改）即换键 miss（上架/改文案自然失效）；未知会话
 * 零请求（空目录短路在缓存之前）；stats 面可观测。借鉴：vLLM radix
 * prefix-cache（内容寻址复用）。
 */
class SkillCatalogCacheTest {

    private static SkillMetadata meta(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

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

    @Test
    void sameCatalogTwiceHitsWithEqualOutput() {
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");
        List<SkillMetadata> catalog = List.of(meta("a", "da"), meta("b", "db"));
        SkillCatalogRendererImpl renderer =
                new SkillCatalogRendererImpl(index, registryOf(catalog));

        Optional<String> first = renderer.renderCatalog("s1");
        Optional<String> second = renderer.renderCatalog("s1");

        assertThat(first).isPresent();
        assertThat(second).containsSame(first.get()); // 命中复用同一实例
        var stats = renderer.renderCacheStats();
        assertThat(stats.requests()).isEqualTo(2);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
    }

    @Test
    void catalogChangeInvalidatesByKey() {
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");
        List<SkillMetadata> mutable = new ArrayList<>(List.of(meta("a", "旧描述")));
        SkillCatalogRendererImpl renderer =
                new SkillCatalogRendererImpl(index, registryOf(mutable));

        renderer.renderCatalog("s1");
        mutable.set(0, meta("a", "新描述")); // 改文案：键变——自然失效
        Optional<String> after = renderer.renderCatalog("s1");

        assertThat(after).hasValueSatisfying(text -> assertThat(text).contains("新描述"));
        var stats = renderer.renderCacheStats();
        assertThat(stats.misses()).isEqualTo(2); // 两次都是 miss（内容变了）
        assertThat(stats.hits()).isZero();
    }

    @Test
    void unknownSessionShortCircuitsBeforeCache() {
        SessionBindingIndex index = new SessionBindingIndex();
        SkillCatalogRendererImpl renderer = new SkillCatalogRendererImpl(index,
                registryOf(List.of(meta("a", "da"))));
        assertThat(renderer.renderCatalog("ghost")).isEmpty();
        assertThat(renderer.renderCacheStats().requests()).isZero();
    }
}
