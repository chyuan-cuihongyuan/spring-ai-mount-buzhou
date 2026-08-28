package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 会话轨迹 → 评估数据集回流（spec 72 §A / T295 / effort#32，LangSmith
 * 「session/trace 转 dataset」思想）：把既有会话的完整轮次（user 输入 + assistant
 * 回复）批量转为评估项（input=问、expected=答、溯源=会话+轮次）。黄金轨迹回归场景：
 * 从已知良好会话一键建集（调用方筛会话——golden 与否是调用方判断，机制不预设）。
 *
 * <p><b>口径</b>（与 {@link FeedbackImporter} 同源）：轮内取首条 USER 文本与首条
 * ASSISTANT 文本；缺任一跳过（skippedMissingTurn）；数据集内同 (sessionId, turnSeq)
 * 溯源去重（skippedDuplicate）；dataset 未建 fail-fast。工具中间轮（多轮
 * think→tool 递归）按顶层轮序回流——与负反馈回流同一顶层口径。
 *
 * @since 1.0.0
 */
public final class SessionTrajectoryImporter {

    private final MessageStore messageStore;
    private final EvalDatasetStore datasetStore;

    public SessionTrajectoryImporter(MessageStore messageStore, EvalDatasetStore datasetStore) {
        this.messageStore = messageStore;
        this.datasetStore = datasetStore;
    }

    /** 回流计数：imported 新入集 / skippedDuplicate 同溯源已存在 / skippedIncomplete 轮缺问或缺答。 */
    public record TrajectoryImportResult(int imported, int skippedDuplicate, int skippedIncomplete) {
    }

    /** 执行回流（dataset 未建 fail-fast 挂 EVAL_OPERATION_INVALID）。 */
    public TrajectoryImportResult importFromSession(String sessionId, String datasetName) {
        if (datasetStore.dataset(datasetName).isEmpty()) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.EVAL_OPERATION_INVALID,
                    "数据集未建：" + datasetName + "（修法：先 createDataset 再回流）");
        }
        Set<String> existing = new HashSet<>();
        for (EvalItem item : datasetStore.items(datasetName)) {
            if (item.sourceSessionId() != null && item.sourceTurnSeq() != null) {
                existing.add(item.sourceSessionId() + "#" + item.sourceTurnSeq());
            }
        }
        List<BuzhouMessage> history = messageStore.load(sessionId);
        TreeSet<Integer> turns = new TreeSet<>();
        for (BuzhouMessage m : history) {
            turns.add(m.turnSeq());
        }
        int imported = 0;
        int duplicate = 0;
        int incomplete = 0;
        for (int turnSeq : turns) {
            String input = firstText(history, turnSeq, Role.USER);
            String expected = firstText(history, turnSeq, Role.ASSISTANT);
            if (input == null || expected == null) {
                incomplete++;
                continue;
            }
            if (existing.contains(sessionId + "#" + turnSeq)) {
                duplicate++;
                continue;
            }
            datasetStore.addItem(datasetName, input, expected, sessionId, turnSeq);
            imported++;
        }
        return new TrajectoryImportResult(imported, duplicate, incomplete);
    }

    private static String firstText(List<BuzhouMessage> history, int turnSeq, Role role) {
        for (BuzhouMessage m : history) {
            if (m.turnSeq() == turnSeq && m.role() == role
                    && m.content() != null && !m.content().isBlank()) {
                return m.content();
            }
        }
        return null;
    }
}
