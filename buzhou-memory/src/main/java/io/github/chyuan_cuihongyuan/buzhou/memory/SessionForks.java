package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.TurnSpan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * time-travel fork（wayfinder2 impl-09 / T34 / docs/spec/12 §core-6，LangGraph
 * get_state_history/fork 的 Buzhou 形状）：Completed-Turn 即检查点——枚举历史检查点
 * （指纹 + 元数据）、从任一检查点复制截至该 Turn 的 history 到新 sessionId 续跑
 * （Buzhou state = 消息列表，无需 channel 版本机制）；原会话不受影响（隔离分叉）。
 */
public final class SessionForks {

    /** 检查点：完结 Turn 的元数据 + 历史指纹。 */
    public record TurnCheckpoint(int turnSeq, int messageCount, String fingerprint) {
    }

    private final MessageStore messageStore;
    private final CompletedTurnDetector detector;

    public SessionForks(MessageStore messageStore) {
        this(messageStore, new DefaultCompletedTurnDetector());
    }

    public SessionForks(MessageStore messageStore, CompletedTurnDetector detector) {
        this.messageStore = messageStore;
        this.detector = detector;
    }

    /** 枚举历史 Completed-Turn 检查点（指纹 = 截至该 Turn 全部消息 id 的 sha256）。 */
    public List<TurnCheckpoint> listCheckpoints(String sessionId) {
        List<BuzhouMessage> history = messageStore.load(sessionId);
        List<TurnCheckpoint> checkpoints = new ArrayList<>();
        for (TurnSpan span : detector.detectTurns(history)) {
            if (!span.completed()) {
                continue;
            }
            int turnSeq = history.get(span.startMessageOffset()).turnSeq();
            List<BuzhouMessage> upto = history.stream()
                    .filter(m -> m.turnSeq() <= turnSeq).toList();
            checkpoints.add(new TurnCheckpoint(turnSeq, upto.size(),
                    fingerprint(upto)));
        }
        return checkpoints;
    }

    /** 从检查点分叉：复制截至该 Turn 的 history 到新 sessionId（原会话不动）；返回新 id。 */
    public String forkFrom(String sessionId, int turnSeq) {
        return forkFrom(sessionId, turnSeq, "fork-" + UUID.randomUUID());
    }

    public String forkFrom(String sessionId, int turnSeq, String newSessionId) {
        List<BuzhouMessage> history = messageStore.load(sessionId).stream()
                .filter(m -> m.turnSeq() <= turnSeq)
                .map(m -> new BuzhouMessage(UUID.randomUUID().toString(), newSessionId,
                        m.turnSeq(), m.seqInTurn(), m.role(), m.content(), m.toolCalls(),
                        m.toolCallId(), m.reasoningContent(), m.reasoningSignature(),
                        m.metadata(), m.createdAt()))
                .toList();
        messageStore.append(newSessionId, history);
        return newSessionId;
    }

    private static String fingerprint(List<BuzhouMessage> messages) {
        StringBuilder joined = new StringBuilder();
        for (BuzhouMessage message : messages) {
            joined.append(message.id()).append('|');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(joined.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
