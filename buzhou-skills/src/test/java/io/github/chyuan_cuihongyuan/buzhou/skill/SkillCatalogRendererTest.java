package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link SkillCatalogRendererImpl} 渲染测试（spec 04 清单注入）。 */
class SkillCatalogRendererTest {

    @Test
    void rendersBoundCatalogForSession() {
        BindingPolicyStore store = new InMemoryBindingPolicyStore();
        store.save(new BindingPolicy("app", "agent", Map.of(), List.of("sql-tuning"), List.of(), 1));
        SkillRegistry registry = registry(store);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");

        Optional<String> catalog = new SkillCatalogRendererImpl(index, registry).renderCatalog("sess");

        assertThat(catalog).isPresent();
        assertThat(catalog.get()).contains("可用技能", "load_skill(name)", "sql-tuning: 慢 SQL 诊断");
        // 未绑定的 code-review 被裁剪
        assertThat(catalog.get()).doesNotContain("code-review");
    }

    @Test
    void unboundSessionRendersAllClasspath() {
        SkillRegistry registry = registry(null);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");

        Optional<String> catalog = new SkillCatalogRendererImpl(index, registry).renderCatalog("sess");

        assertThat(catalog).isPresent();
        assertThat(catalog.get()).contains("code-review", "sql-tuning");
    }

    @Test
    void emptyCatalogWhenBindingCropsToNothing() {
        BindingPolicyStore store = new InMemoryBindingPolicyStore();
        store.save(new BindingPolicy("app", "agent", Map.of(), List.of("nonexistent"), List.of(), 1));
        SkillRegistry registry = registry(store);
        SessionBindingIndex index = new SessionBindingIndex();
        index.register("sess", "app", "agent");

        // 绑定的技能名不存在 → 清单为空 → 不注入
        assertThat(new SkillCatalogRendererImpl(index, registry).renderCatalog("sess")).isEmpty();
    }

    @Test
    void unknownSessionRendersEmpty() {
        SkillRegistry registry = registry(null);
        SessionBindingIndex index = new SessionBindingIndex();

        assertThat(new SkillCatalogRendererImpl(index, registry).renderCatalog("never-registered")).isEmpty();
    }

    private DefaultSkillRegistry registry(BindingPolicyStore store) {
        return new DefaultSkillRegistry(cast(new ClasspathSkillScanner().scan()), null,
                store == null ? null : new StoreBackedPolicyProvider(store), false, 64);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> cast(
            Map<String, ?> raw) {
        return (Map) raw;
    }
}
