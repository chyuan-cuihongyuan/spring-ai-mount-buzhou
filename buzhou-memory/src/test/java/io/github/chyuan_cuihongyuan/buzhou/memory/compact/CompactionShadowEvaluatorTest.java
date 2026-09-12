package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 微压缩影子干跑测试（spec 612 / T874–T875 / impl 465，Istio mirroring 思想）：
 * 干跑报告与直压同口径、梯度单调、事件外发形状、历史零变异。
 */
class CompactionShadowEvaluatorTest {

    private final CompactionShadowEvaluator evaluator =
            new CompactionShadowEvaluator(new DefaultMicroCompactor(new DefaultCompletedTurnDetector()));

    private static final Function<String, MicroCompactionPolicy> ALWAYS_COMPRESS =
            name -> MicroCompactionPolicy.defaults();

    private BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private List<BuzhouMessage> twentyTurnSession(String sessionId, int resultChars) {
        List<BuzhouMessage> history = new ArrayList<>();
        String big = "x".repeat(resultChars);
        for (int turn = 1; turn <= 20; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "q" + turn));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, big, List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "query"), Instant.now()));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "回答 " + turn));
        }
        return history;
    }

    /** 干跑报告与直接压缩同口径（ids/chars 一致）；历史零变异（影子不应用）。 */
    @Test
    void shadowReportMatchesDirectCompactionWithoutMutation() {
        List<BuzhouMessage> history = twentyTurnSession("s-" + UUID.randomUUID(), 3000);
        List<BuzhouMessage> untouched = List.copyOf(history);

        CompactionShadowEvaluator.ShadowReport report = evaluator.evaluate(history, 20,
                ALWAYS_COMPRESS, 2, 1.0);
        MicroCompactionResult direct = new DefaultMicroCompactor(new DefaultCompletedTurnDetector())
                .compact(history, 20, ALWAYS_COMPRESS, 2, 1.0);

        assertThat(report.wouldCompactIds()).isEqualTo(direct.compactedMessageIds());
        assertThat(report.reclaimedChars()).isEqualTo(direct.reclaimedChars());
        assertThat(report.wouldCompactIds()).isNotEmpty();  // 有真实可压内容
        assertThat(report.reclaimedChars()).isPositive();
        assertThat(history).isEqualTo(untouched);           // 影子零变异
    }

    /** 梯度 sweep：reclaimedChars 随 evictRatio 单调不减（0.25 ≤ 0.5 ≤ 0.75 ≤ 1.0）。 */
    @Test
    void sweepIsMonotonicInRatio() {
        List<BuzhouMessage> history = twentyTurnSession("s-" + UUID.randomUUID(), 3000);

        List<CompactionShadowEvaluator.ShadowReport> table = evaluator.sweep(history, 20,
                ALWAYS_COMPRESS, 2);

        assertThat(table).hasSize(4);
        assertThat(table.stream().map(CompactionShadowEvaluator.ShadowReport::evictRatio))
                .containsExactly(0.25, 0.5, 0.75, 1.0);
        for (int i = 1; i < table.size(); i++) {
            assertThat(table.get(i).reclaimedChars())
                    .isGreaterThanOrEqualTo(table.get(i - 1).reclaimedChars());
        }
    }

    /** 事件外发形状：candidateCount/reclaimedChars/evictRatio/applied=false。 */
    @Test
    void emitsShadowEventWithAppliedFalse() {
        List<BuzhouMessage> history = twentyTurnSession("s-" + UUID.randomUUID(), 3000);
        List<SessionEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

        evaluator.evaluateAndEmit(history, 20, ALWAYS_COMPRESS, 2, 0.75, events::add);

        assertThat(events).hasSize(1);
        SessionEvent event = events.get(0);
        assertThat(event.type()).isEqualTo("memory.compaction-shadow");
        assertThat(event.payload()).containsEntry("applied", false);
        assertThat(event.payload()).containsEntry("evictRatio", 0.75);
        assertThat((Integer) event.payload().get("candidateCount")).isPositive();
        assertThat((Integer) event.payload().get("reclaimedChars")).isPositive();
    }

    /** null compactor 构造拒绝。 */
    @Test
    void nullCompactorRejected() {
        assertThatThrownBy(() -> new CompactionShadowEvaluator(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
