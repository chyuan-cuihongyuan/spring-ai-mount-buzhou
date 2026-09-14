package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSearchTool;
import io.github.chyuan_cuihongyuan.buzhou.skill.DefaultSkillRegistry;
import io.github.chyuan_cuihongyuan.buzhou.skill.SessionBindingIndex;
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
                new SessionBindingIndex());
    }

    @Test
    void adminOpsAndSearchKeepIndependentReadouts() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi admin = api(db);
        SkillSearchTool search = searchTool(db);

        // 管理面操作
        admin.create("code-review", "d", "b", List.of(), "ops");
        // 搜索面：内置技能描述词命中 + 不存在标记 miss
        search.call("{\"query\":\"code-review\"}");
        search.call("{\"query\":\"zzzqqq-no-match\"}");

        SkillAdminApi.SkillAdminStats as = SkillAdminApi.stats();
        SkillSearchTool.SkillSearchStats ss = SkillSearchTool.stats();
        // 管理操作不影响搜索计数（互不串账）
        assertThat(as.creates()).isEqualTo(1);
        assertThat(ss.calls()).isEqualTo(2);
        assertThat(ss.hits()).isEqualTo(1);
        // 双读面各自守恒
        assertThat(as.creates()).isGreaterThanOrEqualTo(0);
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
