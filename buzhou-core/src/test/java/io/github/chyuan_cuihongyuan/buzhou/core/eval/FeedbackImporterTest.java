package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 52 §B / T191 / impl-157：负反馈回流端到端（口径复用 isNegative + 溯源 + 幂等）。
 */
class FeedbackImporterTest {

    @Test
    void importsOnlyNegativeTurnsWithTraceability() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);
        datasetStore.createDataset("badcases", null);

        // 历史：turn1 user+assistant、turn2 user+assistant、turn3 user+assistant
        appendTurn(stores, "sess-fb", 1, "Q1", "A1");
        appendTurn(stores, "sess-fb", 2, "Q2", "A2");
        appendTurn(stores, "sess-fb", 3, "Q3", "A3");
        // 反馈：turn1 boolean=false（负）、turn2 numeric=5（正）、turn3 categorical=bad（无极性）
        putFeedback(stores, "sess-fb", "buzhou.feedback.1.100-1", "boolean", "false", 1);
        putFeedback(stores, "sess-fb", "buzhou.feedback.2.101-1", "numeric", "5", 2);
        putFeedback(stores, "sess-fb", "buzhou.feedback.3.102-1", "categorical", "bad", 3);

        FeedbackImporter.FeedbackImportResult result = importer.importFromFeedback("sess-fb", "badcases");
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skippedDuplicate()).isZero();
        assertThat(result.skippedMissingReply()).isZero();

        List<EvalItem> items = datasetStore.items("badcases");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).input()).isEqualTo("Q1");
        assertThat(items.get(0).expected()).isEqualTo("A1");
        assertThat(items.get(0).sourceSessionId()).isEqualTo("sess-fb");
        assertThat(items.get(0).sourceTurnSeq()).isEqualTo(1);

        // 幂等：重复回流全跳过
        FeedbackImporter.FeedbackImportResult again = importer.importFromFeedback("sess-fb", "badcases");
        assertThat(again.imported()).isZero();
        assertThat(again.skippedDuplicate()).isEqualTo(1);
        assertThat(datasetStore.items("badcases")).hasSize(1);
    }

    @Test
    void skipsTurnsWithoutAssistantReplyAndFailsFastOnMissingDataset() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        FeedbackImporter importer = new FeedbackImporter(stores.sessionStateStore(),
                stores.messageStore(), datasetStore);

        // 未建 dataset：fail-fast
        assertThatThrownBy(() -> importer.importFromFeedback("sess-x", "missing"))
                .isInstanceOf(BuzhouException.class);

        datasetStore.createDataset("d", null);
        // 负反馈轮 2 只有 user 无 assistant（护栏拦截语义）→ skippedMissingReply
        stores.messageStore().append("sess-x", List.of(msg("sess-x", 2, 1, Role.USER, "只问不答")));
        putFeedback(stores, "sess-x", "buzhou.feedback.2.100-1", "boolean", "false", 2);
        FeedbackImporter.FeedbackImportResult result = importer.importFromFeedback("sess-x", "d");
        assertThat(result.imported()).isZero();
        assertThat(result.skippedMissingReply()).isEqualTo(1);
        assertThat(datasetStore.items("d")).isEmpty();

        // 空反馈会话：零导入零跳过
        FeedbackImporter.FeedbackImportResult empty = importer.importFromFeedback("sess-none", "d");
        assertThat(empty.imported()).isZero();
        assertThat(empty.skippedDuplicate()).isZero();
        assertThat(empty.skippedMissingReply()).isZero();
    }

    // ---- 构造辅助 ----

    private static void appendTurn(BuzhouStores stores, String sessionId, int turnSeq,
            String question, String answer) {
        stores.messageStore().append(sessionId, List.of(
                msg(sessionId, turnSeq, 1, Role.USER, question),
                msg(sessionId, turnSeq, 2, Role.ASSISTANT, answer)));
    }

    private static BuzhouMessage msg(String sessionId, int turnSeq, int seqInTurn, Role role,
            String content) {
        return new BuzhouMessage(sessionId + "-t" + turnSeq + "-" + seqInTurn, sessionId,
                turnSeq, seqInTurn, role, content, List.of(), null, null, null,
                java.util.Map.of(), Instant.now());
    }

    private static void putFeedback(BuzhouStores stores, String sessionId, String key,
            String type, String value, int turnSeq) {
        String encoded = "type=" + type + "&value=" + value + "&source=user";
        stores.sessionStateStore().put(sessionId,
                new StateEntry(key, encoded, "turn-feedback", turnSeq, null, Instant.now()));
    }
}
