package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.FeedbackExporter;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 负反馈回流（spec 52 §B / T191）：scan 会话负反馈（复用
 * {@link FeedbackExporter#isNegative} 单一事实源口径）→ 每个负反馈轮取 user 输入
 * （input）与首条非空 assistant 回复（expected）→ {@link EvalDatasetStore#addItem}
 * （带 sessionId+turnSeq 溯源）；同数据集内同溯源去重（幂等）。
 *
 * <p>无 assistant 回复的负反馈轮（如被护栏拦截的轮）跳过并计数——不造空 expected 项。
 * dataset 必须已存在（fail-fast，防误操作散集）。
 */
public final class FeedbackImporter {

    private final SessionStateStore stateStore;
    private final MessageStore messageStore;
    private final EvalDatasetStore datasetStore;

    public FeedbackImporter(SessionStateStore stateStore, MessageStore messageStore,
            EvalDatasetStore datasetStore) {
        this.stateStore = stateStore;
        this.messageStore = messageStore;
        this.datasetStore = datasetStore;
    }

    /**
     * 回流结果计数：imported = 新入集；skippedDuplicate = 数据集内同溯源已存在；
     * skippedMissingReply = 无 user 输入或无 assistant 回复。
     */
    public record FeedbackImportResult(int imported, int skippedDuplicate, int skippedMissingReply) {
    }

    /** 执行回流（dataset 未建 fail-fast 挂 EVAL_OPERATION_INVALID）。 */
    public FeedbackImportResult importFromFeedback(String sessionId, String datasetName) {
        if (datasetStore.dataset(datasetName).isEmpty()) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.EVAL_OPERATION_INVALID,
                    "数据集未建：" + datasetName + "（修法：先 createDataset 再回流）");
        }
        // 负反馈轮集合（同轮多条负反馈去重，升序）
        TreeSet<Integer> negativeTurns = new TreeSet<>();
        stateStore.scanByPrefix(sessionId, FeedbackExporter.FEEDBACK_PREFIX).forEach((key, entry) -> {
            Map<String, String> fields = FeedbackExporter.decode(entry.value());
            if (FeedbackExporter.isNegative(fields.get("type"), fields.get("value"))) {
                negativeTurns.add(entry.createdTurn());
            }
        });
        // 数据集内既有溯源（去重判定）
        Set<String> existing = new HashSet<>();
        for (EvalItem item : datasetStore.items(datasetName)) {
            if (item.sourceSessionId() != null && item.sourceTurnSeq() != null) {
                existing.add(sourceKey(item.sourceSessionId(), item.sourceTurnSeq()));
            }
        }
        List<BuzhouMessage> history = messageStore.load(sessionId);
        int imported = 0;
        int duplicate = 0;
        int missing = 0;
        for (int turnSeq : negativeTurns) {
            if (existing.contains(sourceKey(sessionId, turnSeq))) {
                duplicate++;
                continue;
            }
            String input = firstText(history, turnSeq, Role.USER);
            String expected = firstText(history, turnSeq, Role.ASSISTANT);
            if (input == null || expected == null) {
                missing++;
                continue;
            }
            datasetStore.addItem(datasetName, input, expected, sessionId, turnSeq);
            existing.add(sourceKey(sessionId, turnSeq));
            imported++;
        }
        return new FeedbackImportResult(imported, duplicate, missing);
    }

    /** 轮内角色首条非空文本（工具调用空 content 消息跳过）。 */
    private static String firstText(List<BuzhouMessage> history, int turnSeq, Role role) {
        return history.stream()
                .filter(m -> m.turnSeq() == turnSeq && m.role() == role)
                .filter(m -> m.content() != null && !m.content().isBlank())
                .map(BuzhouMessage::content)
                .findFirst()
                .orElse(null);
    }

    private static String sourceKey(String sessionId, int turnSeq) {
        return sessionId + "#" + turnSeq;
    }
}
