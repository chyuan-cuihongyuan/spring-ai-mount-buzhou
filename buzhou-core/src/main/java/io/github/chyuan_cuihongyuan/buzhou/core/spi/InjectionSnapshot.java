package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 注入快照（ticket 15：消息序列 + 动态预算明细 + 策略版本）。
 *
 * <p>每轮注入视图构建完成时刻的完整落库副本，后台按轮还原"模型实际所见"。
 */
public record InjectionSnapshot(
        String sessionId,
        int turnSeq,
        List<String> messageIds,
        List<SnapshotMessage> messages,
        Map<String, Object> budgetBreakdown,
        String policyVersion,
        Instant createdAt) {

    public InjectionSnapshot {
        messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
        messages = messages == null ? List.of() : List.copyOf(messages);
        budgetBreakdown = budgetBreakdown == null ? Map.of() : Map.copyOf(budgetBreakdown);
        policyVersion = policyVersion == null ? "" : policyVersion;
    }

    /** 兼容构造（ticket 06 测试用：无 messages / policyVersion）。 */
    public InjectionSnapshot(String sessionId, int turnSeq, List<String> messageIds,
                             Map<String, Object> budgetBreakdown, Instant createdAt) {
        this(sessionId, turnSeq, messageIds, List.of(), budgetBreakdown, "", createdAt);
    }
}

