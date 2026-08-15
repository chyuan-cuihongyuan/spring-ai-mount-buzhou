package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨 store 迁移器测试（spec 38 §B / T136 / impl-109；core 侧同构用例）：
 * 三槽全量搬迁 + 重映射/keepIds + 目标侧续用。跨 store 形态（JDBC→内存）用例在
 * examples（依赖方向：store 模块 → core，core 测试不可反向依赖）。
 */
class SessionMigratorTest {

    /** 同构迁移（内存→内存）：三槽全量 + 新 Id + 目标续用。 */
    @Test
    void migratesAllThreeSlotsWithRemap() {
        ScriptedChatModel sourceModel = new ScriptedChatModel();
        sourceModel.enqueueText("源答复");
        sourceModel.enqueueText("目标续聊");
        BuzhouStores sourceStores = Buzhou.inMemoryStores();
        AgentRuntime source = Buzhou.runtime(sourceModel, sourceStores, RuntimeConfig.defaults());
        AgentSession session = source.spawn("app", "ag", "mig-src");
        session.chat("业务问题");
        sourceStores.summaryStore().save("mig-src", new StructuredSummary(
                "mig-src", 1, Map.of("core", "迁移摘要"), 6, Instant.now()));
        sourceStores.sessionStateStore().put("mig-src", new StateEntry(
                "budget.used", "777", "t", 1, null, Instant.now()));
        session.close();

        BuzhouStores targetStores = Buzhou.inMemoryStores();
        AgentRuntime target = Buzhou.runtime(sourceModel, targetStores, RuntimeConfig.defaults());

        String targetId = SessionMigrator.migrate(source, target, "mig-src", false);

        assertThat(targetId).isNotEqualTo("mig-src");
        SessionExport exported = target.exportSession(targetId);
        assertThat(exported.messages()).hasSize(2);
        assertThat(exported.summary()).isNotNull();
        assertThat(exported.state()).containsKey("budget.used");

        AgentSession resumed = target.spawn("app", "ag", targetId);
        assertThat(resumed.chat("续聊")).isEqualTo("目标续聊"); // 同一 scripted 模型续用
        resumed.close();
    }

    /** keepIds：目标空闲原 Id 落位；重复迁移同 Id fail-fast（不静默覆盖）。 */
    @Test
    void keepIdsSemanticsCarryOver() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a");
        AgentRuntime a = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.defaults());
        AgentRuntime b = Buzhou.runtime(new ScriptedChatModel(), Buzhou.inMemoryStores(),
                RuntimeConfig.defaults());
        AgentSession s = a.spawn("app", "ag", "keep-1");
        s.chat("q");
        s.close();

        assertThat(SessionMigrator.migrate(a, b, "keep-1", true)).isEqualTo("keep-1");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> SessionMigrator.migrate(a, b, "keep-1", true))
                .isInstanceOf(SessionImportException.class);
    }
}
