package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultSkillRegistry} 解析测试（spec 04）：未绑定=全部内置、绑定=裁剪清单、
 * DB-PUBLISHED 覆盖内置、下架/删除后回退内置。
 */
class SkillRegistryTest {

    private final Map<String, ?> classpath = new ClasspathSkillScanner().scan();

    @Test
    void unboundListsAllClasspathSkills() {
        DefaultSkillRegistry registry = newRegistry(null, false);

        List<SkillMetadata> catalog = registry.listFor("app", "agent");
        assertThat(catalog).extracting(SkillMetadata::name)
                .containsExactlyInAnyOrder("code-review", "sql-tuning");
        assertThat(catalog).allMatch(m -> m.source() == SkillSource.CLASSPATH);
    }

    @Test
    void boundListCropsToExplicitNames() {
        BindingPolicyStore store = new InMemoryBindingPolicyStore();
        bind(store, "app", "agent", List.of("sql-tuning"));
        DefaultSkillRegistry registry = newRegistry(new StoreBackedPolicyProvider(store), false);

        List<SkillMetadata> catalog = registry.listFor("app", "agent");
        assertThat(catalog).extracting(SkillMetadata::name).containsExactly("sql-tuning");
    }

    @Test
    void dbPublishedOverridesClasspath() {
        SkillStore db = new InMemorySkillStore();
        DefaultSkillRegistry registry = newRegistry(null, true, db);
        db.save(dbSkill("code-review", "DB 版评审正文", SkillStatus.PUBLISHED));

        Skill loaded = registry.load("app", "agent", "code-review").orElseThrow();
        assertThat(loaded.source()).isEqualTo(SkillSource.DB);
        assertThat(loaded.body()).isEqualTo("DB 版评审正文");
        // 清单标注来源为 DB
        assertThat(registry.listFor("app", "agent")).filteredOn(m -> m.name().equals("code-review"))
                .singleElement().extracting(SkillMetadata::source).isEqualTo(SkillSource.DB);
    }

    @Test
    void dbDisabledFallsBackToClasspath() {
        SkillStore db = new InMemorySkillStore();
        DefaultSkillRegistry registry = newRegistry(null, true, db);
        db.save(dbSkill("code-review", "DB 版", SkillStatus.DISABLED));

        Skill loaded = registry.load("app", "agent", "code-review").orElseThrow();
        assertThat(loaded.source()).isEqualTo(SkillSource.CLASSPATH);
    }

    @Test
    void dbDraftNotVisibleFallsBackToClasspath() {
        SkillStore db = new InMemorySkillStore();
        DefaultSkillRegistry registry = newRegistry(null, true, db);
        db.save(dbSkill("code-review", "草稿", SkillStatus.DRAFT));

        // DRAFT 对运行时不可见 → classpath 版
        Skill loaded = registry.load("app", "agent", "code-review").orElseThrow();
        assertThat(loaded.source()).isEqualTo(SkillSource.CLASSPATH);
    }

    @Test
    void deleteDbRestoresClasspath() {
        SkillStore db = new InMemorySkillStore();
        DefaultSkillRegistry registry = newRegistry(null, true, db);
        db.save(dbSkill("code-review", "DB 版", SkillStatus.PUBLISHED));
        assertThat(registry.load("app", "agent", "code-review").orElseThrow().source()).isEqualTo(SkillSource.DB);

        db.deleteByName("code-review");
        assertThat(registry.load("app", "agent", "code-review").orElseThrow().source())
                .isEqualTo(SkillSource.CLASSPATH);
    }

    @Test
    void loadResourceFromDbThenClasspath() {
        SkillStore db = new InMemorySkillStore();
        DefaultSkillRegistry registry = newRegistry(null, true, db);
        db.save(dbSkill("code-review", "b", SkillStatus.PUBLISHED));
        db.saveResource(new io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillResourceRecord(
                null, "code-review", "checklists/security.md", "text/markdown", "DB 资源内容", 7, null));

        // DB 覆盖时取 DB 资源
        assertThat(registry.loadResource("app", "agent", "code-review", "checklists/security.md"))
                .contains("DB 资源内容");

        // 下架后回退到 classpath 资源
        db.save(dbSkill("code-review", "b", SkillStatus.DISABLED));
        assertThat(registry.loadResource("app", "agent", "code-review", "checklists/security.md"))
                .hasValueSatisfying(v -> assertThat(v).contains("输入是否经校验/转义"));
    }

    @Test
    void unknownSkillLoadsEmpty() {
        DefaultSkillRegistry registry = newRegistry(null, false);
        assertThat(registry.load("app", "agent", "nope")).isEmpty();
    }

    @Test
    void isVisibleForReflectsBinding() {
        // 未绑定：全部 classpath 可见；不存在的名字不可见
        DefaultSkillRegistry unbound = newRegistry(null, false);
        assertThat(unbound.isVisibleFor("app", "agent", "code-review")).isTrue();
        assertThat(unbound.isVisibleFor("app", "agent", "nope")).isFalse();

        // 绑定裁剪：清单外不可见（且不受清单展示上限影响）
        InMemoryBindingPolicyStore bindingStore = new InMemoryBindingPolicyStore();
        bindingStore.save(new BindingPolicy("app", "agent", Map.of(), List.of("sql-tuning"), List.of(), 1));
        DefaultSkillRegistry bound = new DefaultSkillRegistry(castClasspath(classpath), null,
                new StoreBackedPolicyProvider(bindingStore), false, 64);
        assertThat(bound.isVisibleFor("app", "agent", "sql-tuning")).isTrue();
        assertThat(bound.isVisibleFor("app", "agent", "code-review")).isFalse();
    }

    @Test
    void catalogCappedByMaxEntries() {
        DefaultSkillRegistry registry = new DefaultSkillRegistry(
                castClasspath(classpath), null, null, false, 1);
        assertThat(registry.listFor("app", "agent")).hasSize(1);
    }

    private DefaultSkillRegistry newRegistry(PolicyConfigProvider pp, boolean dbEnabled) {
        return new DefaultSkillRegistry(castClasspath(classpath), dbEnabled ? new InMemorySkillStore() : null,
                pp, dbEnabled, 64);
    }

    private DefaultSkillRegistry newRegistry(PolicyConfigProvider pp, boolean dbEnabled, SkillStore db) {
        return new DefaultSkillRegistry(castClasspath(classpath), db, pp, dbEnabled, 64);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> castClasspath(
            Map<String, ?> raw) {
        return (Map) raw;
    }

    private static void bind(BindingPolicyStore store, String appId, String agentName, List<String> skills) {
        store.save(new BindingPolicy(appId, agentName, Map.of(), skills, List.of(), 1));
    }

    private static io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord dbSkill(
            String name, String body, SkillStatus status) {
        return new io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord(
                null, name, "DB skill", List.of(), body, status, "tester", null, null, 0);
    }
}
