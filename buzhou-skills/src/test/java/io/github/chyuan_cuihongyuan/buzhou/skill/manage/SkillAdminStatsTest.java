package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1085 / impl 837：Skill 管理操作读面——create/update/publish/disable/delete
 * 五操作独立计数（无统一守恒，口径同 R63 独立型）、resetForTest 归零。
 * 骨架同 SkillAdminApiTest（ClasspathSkillScanner + InMemorySkillStore）。
 */
class SkillAdminStatsTest {

    private final Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> classpath =
            new ClasspathSkillScanner().scan();

    @BeforeEach
    void reset() {
        SkillAdminApi.resetForTest();
    }

    @Test
    void eachOperationCountsItsBucket() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = new SkillAdminApi(db, classpath, null);

        api.create("code-review", "描述", "正文", List.of(), "ops");
        api.update("code-review", "新描述", null, null);
        api.publish("code-review");
        api.disable("code-review");
        api.delete("code-review");

        SkillAdminApi.SkillAdminStats stats = SkillAdminApi.stats();
        assertThat(stats.creates()).isEqualTo(1);
        assertThat(stats.updates()).isEqualTo(1);
        assertThat(stats.publishes()).isEqualTo(1);
        assertThat(stats.disables()).isEqualTo(1);
        assertThat(stats.deletes()).isEqualTo(1);
    }

    @Test
    void validationFailuresDoNotCount() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = new SkillAdminApi(db, classpath, null);
        // 空名 create 抛 IllegalArgument（既有语义）——不计数
        assertThatThrownByQuietly(() -> api.create(null, "d", "b", List.of(), "ops"));
        // 重复 create 抛出——不计数
        api.create("dup", "d", "b", List.of(), "ops");
        assertThatThrownByQuietly(() -> api.create("dup", "d", "b", List.of(), "ops"));

        SkillAdminApi.SkillAdminStats stats = SkillAdminApi.stats();
        assertThat(stats.creates()).isEqualTo(1);
        assertThat(stats.updates()).isZero();
    }

    private void assertThatThrownByQuietly(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException expected) {
            // 既有校验异常语义，本测试只关心计数不入桶
        }
    }

    @Test
    void resetForTestZeroesCounters() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = new SkillAdminApi(db, classpath, null);
        api.create("code-review", "d", "b", List.of(), "ops");
        assertThat(SkillAdminApi.stats().creates()).isEqualTo(1);

        SkillAdminApi.resetForTest();

        SkillAdminApi.SkillAdminStats stats = SkillAdminApi.stats();
        assertThat(stats.creates()).isZero();
        assertThat(stats.updates()).isZero();
        assertThat(stats.publishes()).isZero();
        assertThat(stats.disables()).isZero();
        assertThat(stats.deletes()).isZero();
    }
}
