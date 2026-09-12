package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 渲染节拍漂移巡查测试（spec 629 / T908–T909 / impl 482，617 看门狗接线）：
 * 渲染器带 watcher 时每轮 renderCatalog 顺带 check——首拍建基线、目录变化即漂移事件、
 * 无 watcher 零变化。走真实渲染路径（绑定索引 + registry 替身）。
 */
class RendererDriftWatchTest {

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

    /** 渲染两轮建基线 → 目录改描述 → 再渲染即漂移事件（载荷三分类）。 */
    @Test
    void renderCadenceChecksDrift() {
        List<SkillMetadata> catalog = new ArrayList<>(List.of(
                meta("deploy", "部署"), meta("audit", "审计 v1")));
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        SkillCatalogDriftWatcher watcher = new SkillCatalogDriftWatcher(events::add);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");
        var renderer = new SkillCatalogRendererImpl(index, registryOf(catalog),
                null, Integer.MAX_VALUE, watcher);

        assertThat(renderer.renderCatalog("s1")).isPresent(); // 首拍建基线
        assertThat(renderer.renderCatalog("s1")).isPresent(); // 无变化静默
        assertThat(events).isEmpty();

        catalog.set(1, meta("audit", "审计 v2")); // 改描述（契约面）
        assertThat(renderer.renderCatalog("s1")).isPresent();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("changed")).isEqualTo(List.of("audit"));
    }

    /** 无 watcher 构造：渲染零变化（既有路径）。 */
    @Test
    void withoutWatcherUnchanged() {
        List<SkillMetadata> catalog = List.of(meta("deploy", "部署"));
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("s1", "app", "agent");
        var renderer = new SkillCatalogRendererImpl(index, registryOf(catalog),
                null, Integer.MAX_VALUE);
        assertThat(renderer.renderCatalog("s1")).isPresent(); // 正常渲染不炸
    }
}
