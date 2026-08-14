package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SkillStore 契约测试基类（impl-51 / spec 14 §G，沿 {@code AbstractBuzhouStoresContractTest} 范式）：
 * 全部实现（InMemory / JDBC / Redis）须过同一组行为断言——只测外部行为，不测实现细节。
 */
public abstract class AbstractSkillStoreContractTest {

    protected abstract SkillStore store();

    @Test
    public void saveAndFindRoundTrip() {
        SkillStore store = store();
        DbSkillRecord saved = store.save(record("deploy-guide", "部署指南", SkillStatus.PUBLISHED));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.version()).isGreaterThanOrEqualTo(0);
        assertThat(store.findByName("deploy-guide")).isPresent();
        assertThat(store.findPublished("deploy-guide")).isPresent();
        assertThat(store.findAll()).hasSize(1);
    }

    @Test
    public void draftNotVisibleToRuntime() {
        SkillStore store = store();
        store.save(record("draft-skill", null, SkillStatus.DRAFT));

        assertThat(store.findByName("draft-skill")).isPresent();
        assertThat(store.findPublished("draft-skill")).isEmpty();
    }

    @Test
    public void optimisticLockRejectsStaleVersion() {
        SkillStore store = store();
        DbSkillRecord saved = store.save(record("locked", null, SkillStatus.DRAFT));
        DbSkillRecord concurrent = store.save(new DbSkillRecord(saved.id(), saved.name(),
                saved.description(), saved.allowedTools(), saved.body(), SkillStatus.PUBLISHED,
                saved.createdBy(), saved.createdAt(), Instant.now(), saved.version()));

        assertThat(concurrent.version()).isGreaterThan(saved.version());
        // 用旧 version 再改：冲突
        assertThatThrownBy(() -> store.save(withBody(saved, "stale")))
                .isInstanceOf(SkillVersionConflictException.class);
    }

    @Test
    public void resourceCrudAndCascadeDelete() {
        SkillStore store = store();
        store.save(record("resourced", null, SkillStatus.PUBLISHED));
        store.saveResource(resource("resourced", "docs/api.md", "# api"));

        assertThat(store.findResource("resourced", "docs/api.md")).isPresent();
        assertThat(store.findResources("resourced")).hasSize(1);

        assertThat(store.deleteByName("resourced")).isTrue();
        assertThat(store.findResources("resourced")).isEmpty();
        assertThat(store.deleteByName("resourced")).isFalse();
    }

    // ---- fixtures ----

    protected static DbSkillRecord record(String name, String description, SkillStatus status) {
        return new DbSkillRecord(null, name, description, List.of("read_file"),
                "# " + name + "\n正文", status, "tester", Instant.now(), Instant.now(), 0);
    }

    protected static DbSkillRecord withBody(DbSkillRecord base, String body) {
        return new DbSkillRecord(base.id(), base.name(), base.description(), base.allowedTools(),
                body, base.status(), base.createdBy(), base.createdAt(), Instant.now(),
                base.version());
    }

    protected static DbSkillResourceRecord resource(String skillName, String path, String content) {
        return new DbSkillResourceRecord(null, skillName, path, "text/markdown", content,
                content.length(), Instant.now());
    }
}
