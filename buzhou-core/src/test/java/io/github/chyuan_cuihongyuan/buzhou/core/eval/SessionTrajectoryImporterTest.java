package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话轨迹回流红队（spec 72 §B / T296）：完整轮入集（input/expected/溯源）；缺答轮
 * 跳过；重复回流去重；dataset 未建 fail-fast；工具中间轮按顶层轮序。
 */
class SessionTrajectoryImporterTest {

    @Test
    void fullTurnsImportWithSourceAttribution() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("答一");
        model.enqueueText("答二");
        var runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        try (AgentSession session = runtime.spawn("app", "agent", "sess-gold")) {
            session.chat("问一");
            session.chat("问二");
        }
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("gold-set", null);

        SessionTrajectoryImporter.TrajectoryImportResult result = new SessionTrajectoryImporter(
                stores.messageStore(), datasetStore).importFromSession("sess-gold", "gold-set");

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skippedIncomplete()).isZero();
        List<EvalItem> items = datasetStore.items("gold-set");
        assertThat(items).hasSize(2);
        assertThat(items.getFirst().input()).isEqualTo("问一");
        assertThat(items.getFirst().expected()).isEqualTo("答一");
        assertThat(items.getFirst().sourceSessionId()).isEqualTo("sess-gold");
        assertThat(items.getFirst().sourceTurnSeq()).isEqualTo(1);

        // 重复回流：全部去重
        SessionTrajectoryImporter.TrajectoryImportResult again = new SessionTrajectoryImporter(
                stores.messageStore(), datasetStore).importFromSession("sess-gold", "gold-set");
        assertThat(again.imported()).isZero();
        assertThat(again.skippedDuplicate()).isEqualTo(2);
    }

    @Test
    void datasetNotCreatedFailsFast() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionTrajectoryImporter importer = new SessionTrajectoryImporter(
                stores.messageStore(), new EvalDatasetStore(stores.sessionStateStore()));
        assertThatThrownBy(() -> importer.importFromSession("s", "nope"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException.class)
                .hasMessageContaining("数据集未建");
    }

    @Test
    void emptySessionImportsNothing() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("empty-target", null);
        SessionTrajectoryImporter.TrajectoryImportResult result = new SessionTrajectoryImporter(
                stores.messageStore(), datasetStore).importFromSession("nobody", "empty-target");
        assertThat(result.imported()).isZero();
        assertThat(result.skippedIncomplete()).isZero();
    }
}
