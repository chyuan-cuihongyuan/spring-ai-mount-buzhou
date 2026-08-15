package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 压缩事件化测试（spec 34 §A / T115 / impl-90）：微压缩实际折入时经监听器观测双写
 * `memory.compacted`（compactedCount/reclaimedChars）；无折入零事件；监听器异常不影响视图。
 */
class CompactionEventTest {

    /** 折叠发生 → memory.compacted 事件入观测库（sessionId 对齐 + 计数正确）。 */
    @Test
    void compactionEmitsEventToObservability() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "evt-" + UUID.randomUUID();
        InjectionViewProcessor ivp = processor(stores);
        ivp.setCompactionListener((sid, result, ratio) -> stores.observabilityStore().saveEvents(
                List.of(new EventRecord(UUID.randomUUID().toString(), null, sid,
                        "memory.compacted", Instant.now(),
                        Map.of("compactedCount", result.compactedMessageIds().size(),
                                "reclaimedChars", result.reclaimedChars(),
                                "evictRatio", ratio)))));

        List<BuzhouMessage> folded = ivp.process(sessionId, bigHistory(sessionId, 30), 31);

        assertThat(folded).isNotNull();
        List<EventRecord> events = stores.observabilityStore().eventsOfSession(sessionId);
        assertThat(events).isNotEmpty();
        EventRecord event = events.stream()
                .filter(e -> "memory.compacted".equals(e.type())).findFirst().orElseThrow();
        assertThat(((Number) event.payload().get("compactedCount")).intValue()).isPositive();
        assertThat(((Number) event.payload().get("reclaimedChars")).intValue()).isPositive();
        assertThat(event.payload()).containsKey("evictRatio"); // spec 38 §A：梯子级可区分
    }

    /** 无折叠（小历史）→ 零事件。 */
    @Test
    void noFoldNoEvent() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "quiet-" + UUID.randomUUID();
        InjectionViewProcessor ivp = processor(stores);
        ivp.setCompactionListener((sid, result, ratio) -> stores.observabilityStore().saveEvents(
                List.of(new EventRecord(UUID.randomUUID().toString(), null, sid,
                        "memory.compacted", Instant.now(), Map.of()))));

        ivp.process(sessionId, List.of(msg(sessionId, 1, 0, Role.USER, "hi"),
                msg(sessionId, 1, 1, Role.ASSISTANT, "ok")), 2);

        assertThat(stores.observabilityStore().eventsOfSession(sessionId)).isEmpty();
    }

    /** 监听器抛异常不影响视图主链（lenient）。 */
    @Test
    void listenerFailureNeverBreaksView() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "broken-" + UUID.randomUUID();
        InjectionViewProcessor ivp = processor(stores);
        ivp.setCompactionListener((sid, result, ratio) -> {
            throw new IllegalStateException("观测后端不可用");
        });

        assertThat(ivp.process(sessionId, bigHistory(sessionId, 30), 31)).isNotNull();
    }

    // ---- helpers（CompactionCheckpointTest 同款构造） ----

    private InjectionViewProcessor processor(BuzhouStores stores) {
        DefaultBudgetCalculator budgetCalculator = new DefaultBudgetCalculator(
                new TableContextWindowResolver(Map.of("tiny", 13000)),
                new CharHeuristicTokenEstimator());
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                name -> MicroCompactionPolicy.defaults(), 1,
                budgetCalculator, new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), summaryModel(),
                "tiny", 2, null, 4000);
        ivp.setSessionStateStore(stores.sessionStateStore());
        ivp.setCheckpoints(new CompactionCheckpoints(stores.sessionStateStore()));
        return ivp;
    }

    private static ChatModel summaryModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("## CURRENT_STATE\nok"))));
            }
        };
    }

    private List<BuzhouMessage> bigHistory(String sessionId, int turns) {
        List<BuzhouMessage> history = new ArrayList<>();
        String big = "x".repeat(3000);
        for (int turn = 1; turn <= turns; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "第 " + turn + " 步"));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, big, List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "query"), Instant.now()));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "完成 " + turn));
        }
        return history;
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }
}
