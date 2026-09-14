package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSearchTool;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1095 / impl 847：SkillAdmin×Search 可见性联动组合——create+publish 后
 * 搜索命中（hits）、disable 后搜索消失（misses）、双读面各自守恒。纯测试轮。
 */
class SkillAdminSearchComboTest {

    private final Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> classpath =
            new ClasspathSkillScanner().scan();

    @BeforeEach
    void reset() {
        SkillAdminApi.resetForTest();
        SkillSearchTool.resetForTest();
    }

    private SkillAdminApi api(SkillStore db) {
        return new SkillAdminApi(db, classpath, null);
    }

    private SkillSearchTool searchTool(SkillStore db) {
        return new SkillSearchTool(
                new DefaultSkillRegistry(classpath, db, null, true, 64),
                new io.github.chyuan_cuihongyuan.buzhou.skill.SessionBindingIndex());
    }

    @Test
    void publishThenSearchHitsDisableThenSearchMisses() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi admin = api(db);
        SkillSearchTool search = searchTool(db);

        // 发布 DB skill（覆盖内置同名）
        admin.create("code-review", "联动测试专用标记词 xyzzy", "正文", List.of(), "ops");
        admin.publish("code-review");

        // 搜索命中标记词
        search.call("{\"query\":\"xyzzy\"}");
        SkillSearchTool.SkillSearchStats ss = SkillSearchTool.stats();
        assertThat(ss.hits()).isEqualTo(1);

        // 下架 → 再搜索消失
        admin.disable("code-review");
        search.call("{\"query\":\"xyzzy\"}");
        assertThat(ss.misses()).isEqualTo(1);

        // 双读面各自守恒
        SkillAdminApi.SkillAdminStats as = SkillAdminApi.stats();
        assertThat(as.creates()).isEqualTo(1);
        assertThat(as.publishes()).isEqualTo(1);
        assertThat(as.disables()).isEqualTo(1);
        assertThat(ss.calls()).isEqualTo(ss.hits() + ss.misses()
                + ss.parseRejects() + ss.blankQueryRejects());
    }

    @Test
    void resetIsolatesBothReadouts() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi admin = api(db);
        admin.create("code-review", "d", "b", List.of(), "ops");

        SkillAdminApi.resetForTest();
        assertThat(SkillAdminApi.stats().creates()).isZero();
        assertThat(SkillSearchTool.stats().calls()).isZero();
    }
}
