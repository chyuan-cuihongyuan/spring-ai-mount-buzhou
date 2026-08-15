package io.github.chyuan_cuihongyuan.buzhou.skill.manage;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.DefaultSkillRegistry;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillMetadata;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillRegistry;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillSource;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link SkillAdminApi} 管理 API 测试（spec 04 CRUD/上架/下架/绑定）。 */
class SkillAdminApiTest {

    private final Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> classpath =
            new ClasspathSkillScanner().scan();

    @Test
    void createStartsAsDraftNotVisibleToRuntime() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);

        api.create("code-review", "DB 覆盖", "DB 正文", List.of("read_file"), "ops");

        assertThat(db.findByName("code-review").orElseThrow().status()).isEqualTo(SkillStatus.DRAFT);
        // DRAFT 对运行时不可见 → 仍是 classpath 版
        SkillRegistry registry = registry(db);
        assertThat(registry.load("app", "agent", "code-review").orElseThrow().source())
                .isEqualTo(SkillSource.CLASSPATH);
    }

    @Test
    void publishMakesDbOverrideVisible() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB 覆盖", "DB 正文", List.of(), "ops");
        api.publish("code-review");

        SkillRegistry registry = registry(db);
        assertThat(registry.load("app", "agent", "code-review").orElseThrow().body()).isEqualTo("DB 正文");
    }

    @Test
    void disableRestoresClasspath() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB 覆盖", "DB 正文", List.of(), "ops");
        api.publish("code-review");
        api.disable("code-review");

        assertThat(registry(db).load("app", "agent", "code-review").orElseThrow().source())
                .isEqualTo(SkillSource.CLASSPATH);
    }

    @Test
    void deleteRemovesDbSkill() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("custom-skill", "自定义", "正文", List.of(), "ops");
        api.publish("custom-skill");
        assertThat(api.delete("custom-skill")).isTrue();
        assertThat(db.findByName("custom-skill")).isEmpty();
        assertThat(api.delete("custom-skill")).isFalse();
    }

    @Test
    void updateEditsDbSkillBody() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "old", "old body", List.of(), "ops");
        api.update("code-review", "new desc", "new body", List.of("a"));

        DbSkillRecord updated = db.findByName("code-review").orElseThrow();
        assertThat(updated.description()).isEqualTo("new desc");
        assertThat(updated.body()).isEqualTo("new body");
        assertThat(updated.allowedTools()).containsExactly("a");
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    void updateThrowsForClasspathOnlySkill() {
        SkillAdminApi api = api(new InMemorySkillStore(), null);
        assertThatThrownBy(() -> api.update("code-review", "d", "b", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB Skill 不存在");
    }

    @Test
    void listAllMergesDbAndClasspathWithOverrideAnnotation() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB 版", "b", List.of(), "ops"); // DRAFT（未上架）

        List<SkillSummary> all = api.listAll();
        // code-review 被 DB 条目覆盖展示（即使 DRAFT），sql-tuning 仍 classpath
        assertThat(all).extracting(SkillSummary::name)
                .containsExactlyInAnyOrder("code-review", "sql-tuning");
        SkillSummary codeReview = all.stream().filter(s -> s.name().equals("code-review"))
                .findFirst().orElseThrow();
        assertThat(codeReview.source()).isEqualTo(SkillSource.DB);
        assertThat(codeReview.dbOverridesClasspath()).isTrue();
    }

    @Test
    void bindingRoundTripAndNextTurnEffective() {
        // 经 SkillModule 真实接线：bindingStore 同时供给 AdminApi（写）与 Registry（读穿）
        io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule skills =
                io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule.builder()
                        .bindingStore(new InMemoryBindingPolicyStore()).build();
        SkillAdminApi api = skills.skillAdminApi();

        assertThat(api.getBinding("app", "agent")).isEmpty();
        api.setBinding("app", "agent", List.of("sql-tuning"));
        assertThat(api.getBinding("app", "agent")).containsExactly("sql-tuning");

        // 改绑定经 PolicyConfigProvider 对 registry 立即可见（下一轮清单生效）
        SkillRegistry registry = skills.skillRegistry();
        assertThat(registry.listFor("app", "agent")).extracting(SkillMetadata::name)
                .containsExactly("sql-tuning");
        assertThat(registry.listFor("app", "agent")).extracting(SkillMetadata::name)
                .doesNotContain("code-review");
    }

    @Test
    void uploadResourceReadableByRegistry() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB", "b", List.of(), "ops");
        api.publish("code-review");
        api.uploadResource("code-review", "templates/report.tpl", "模板内容", "text/plain");

        SkillRegistry registry = registry(db);
        assertThat(registry.loadResource("app", "agent", "code-review", "templates/report.tpl"))
                .contains("模板内容");
    }

    @Test
    void createDuplicateNameRejected() {
        SkillAdminApi api = api(new InMemorySkillStore(), null);
        api.create("code-review", "DB 覆盖", "正文", List.of(), "ops");
        assertThatThrownBy(() -> api.create("code-review", "再建", "b", List.of(), "ops"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void publishAlreadyPublishedRejected() {
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB 覆盖", "正文", List.of(), "ops");
        api.publish("code-review");
        assertThatThrownBy(() -> api.publish("code-review"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException.class)
                .hasMessageContaining("已是上架状态")
                .extracting("errorCode")
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SKILL_OPERATION_INVALID);
    }

    @Test
    void disableDraftRejected() {
        SkillAdminApi api = api(new InMemorySkillStore(), null);
        api.create("code-review", "DB 覆盖", "正文", List.of(), "ops");
        assertThatThrownBy(() -> api.disable("code-review"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException.class)
                .hasMessageContaining("仅上架状态可下架")
                .extracting("errorCode")
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.SKILL_OPERATION_INVALID);
    }

    @Test
    void republishDisabledAllowed() {
        // 推演：DISABLED → PUBLISHED 重新上架（否则下架不可逆）
        SkillStore db = new InMemorySkillStore();
        SkillAdminApi api = api(db, null);
        api.create("code-review", "DB 覆盖", "正文", List.of(), "ops");
        api.publish("code-review");
        api.disable("code-review");
        api.publish("code-review");

        assertThat(registry(db).load("app", "agent", "code-review").orElseThrow().source())
                .isEqualTo(SkillSource.DB);
    }

    private SkillAdminApi api(SkillStore db, BindingPolicyStore bindingStore) {
        return new SkillAdminApi(db, classpath, bindingStore);
    }

    private SkillRegistry registry(SkillStore db) {
        return new DefaultSkillRegistry(classpath, db, null, db != null, 64);
    }
}
