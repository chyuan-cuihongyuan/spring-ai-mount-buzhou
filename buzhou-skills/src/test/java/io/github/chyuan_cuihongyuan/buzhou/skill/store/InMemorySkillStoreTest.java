package io.github.chyuan_cuihongyuan.buzhou.skill.store;

import io.github.chyuan_cuihongyuan.buzhou.skill.SkillStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link InMemorySkillStore} version 乐观锁契约测试（spec 04：并发编辑兜底）。 */
class InMemorySkillStoreTest {

    private final InMemorySkillStore store = new InMemorySkillStore();

    @Test
    void createThenUpdateBumpsVersion() {
        DbSkillRecord created = store.save(record("s", 0));
        assertThat(created.version()).isZero();

        DbSkillRecord updated = store.save(record("s", created.version()));
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    void staleVersionRejected() {
        DbSkillRecord created = store.save(record("s", 0));
        store.save(record("s", created.version())); // 库内推进到 version=1

        // 拿着过期 version=0 再写 → 冲突，不静默覆盖
        assertThatThrownBy(() -> store.save(record("s", 0)))
                .isInstanceOf(SkillVersionConflictException.class)
                .hasMessageContaining("version 冲突");
        assertThat(store.findByName("s").orElseThrow().version()).isEqualTo(1);
    }

    private static DbSkillRecord record(String name, int version) {
        return new DbSkillRecord(null, name, "d", List.of(), "b", SkillStatus.DRAFT,
                "ops", null, null, version);
    }
}
