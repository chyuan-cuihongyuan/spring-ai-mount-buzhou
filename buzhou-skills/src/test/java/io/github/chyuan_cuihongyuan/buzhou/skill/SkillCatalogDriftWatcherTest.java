package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 技能目录漂移看门狗测试（spec 617 / T884–T885 / impl 470，spec 201 镜像）：
 * 首拍建基线不发事件、变化发事件且基线推进、无变化静默、事件载荷形状。
 */
class SkillCatalogDriftWatcherTest {

    private static SkillMetadata skill(String name, String description) {
        return new SkillMetadata(name, description, List.of(), SkillSource.CLASSPATH);
    }

    /** 首拍建基线（无事件）；同目录再查静默；变化发事件且载荷含三分类+摘要。 */
    @Test
    void baselineThenSilentThenDriftEmitsAndAdvances() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        SkillCatalogDriftWatcher watcher = new SkillCatalogDriftWatcher(events::add);

        assertThat(watcher.check(List.of(skill("a", "v1"), skill("b", "d")))).isEmpty(); // 首拍建基线
        assertThat(events).isEmpty();
        assertThat(watcher.baselineSummary()).hasSize(64);
        String firstBaseline = watcher.baselineSummary();

        assertThat(watcher.check(List.of(skill("a", "v1"), skill("b", "d")))).isEmpty(); // 无变化静默

        assertThat(watcher.check(List.of(skill("a", "v2"), skill("c", "n")))).isPresent(); // a 改 b 删 c 增
        assertThat(events).hasSize(1);
        Map<String, Object> payload = events.get(0);
        assertThat(payload.get("added")).isEqualTo(List.of("c"));
        assertThat(payload.get("removed")).isEqualTo(List.of("b"));
        assertThat(payload.get("changed")).isEqualTo(List.of("a"));
        assertThat(payload.get("oldSummary")).isEqualTo(firstBaseline);
        assertThat((String) payload.get("newSummary")).hasSize(64);

        // 基线已推进：同目录再查静默（不重放旧闻）
        assertThat(watcher.check(List.of(skill("a", "v2"), skill("c", "n")))).isEmpty();
        assertThat(events).hasSize(1);
    }

    /** 空 emitter 安全（观测面缺席不炸）；null 清单按空目录处理。 */
    @Test
    void nullEmitterAndNullCatalogSafe() {
        SkillCatalogDriftWatcher watcher = new SkillCatalogDriftWatcher(null);
        assertThat(watcher.check(null)).isEmpty();          // 首拍空基线
        assertThat(watcher.check(List.of(skill("x", "d")))).isPresent(); // 增=漂移
    }
}
