package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupGate;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeyExtractor;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.IdempotencyKeys;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 幂等去重接入恢复重放（ticket 04）：修复器先查去重记录的决策表（纯函数形态）。
 */
class DanglingCallRepairerDedupTest {

    private static final String SESSION = "s";

    private final InMemorySessionStateStore stateStore = new InMemorySessionStateStore();
    private final DedupRecorder recorder = new DedupRecorder(stateStore);
    private final List<DanglingCallRepairer.RepairEvent> repairEvents = new ArrayList<>();
    private final List<SessionEvent> sessionEvents = new ArrayList<>();

    private DanglingCallRepairer repairer(Map<String, ToolCallback> tools, Set<String> idempotent,
                                          Map<String, IdempotencyKeyExtractor> extractors) {
        DedupGate gate = new DedupGate(recorder, extractors, sessionEvents::add);
        return new DanglingCallRepairer(tools, idempotent, (sid, e) -> repairEvents.add(e), gate);
    }

    private static ToolCallback tool(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name).inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return result;
            }
        };
    }

    private static BuzhouMessage danglingAssistant(String toolCallId, String toolName, String arguments) {
        return new BuzhouMessage(UUID.randomUUID().toString(), SESSION, 1, 2, Role.ASSISTANT, "我来处理",
                List.of(new ToolCallRecord(toolCallId, toolName, arguments)), null, null, null,
                Map.of(), Instant.now());
    }

    @Test
    void dedupHitSynthesizesStoredResultWithoutReExecution_evenForSideEffectTool() {
        AtomicInteger calls = new AtomicInteger();
        // 非幂等副作用工具：崩溃窗口「已执行、结果未落库」——去重记录已回填
        recorder.fill(SESSION, IdempotencyKeys.defaultKey("charge", "tc-1"), "charged-100");
        var repairer = repairer(Map.of("charge", tool("charge", calls, "charged-again")), Set.of(), Map.of());

        List<BuzhouMessage> repaired = repairer.repair(SESSION, List.of(danglingAssistant("tc-1", "charge", "{}")));

        // 用存储结果合成工具响应、不重执行（恰好一次）
        assertThat(calls).hasValue(0);
        BuzhouMessage synthesized = repaired.get(1);
        assertThat(synthesized.role()).isEqualTo(Role.TOOL);
        assertThat(synthesized.toolCallId()).isEqualTo("tc-1");
        assertThat(synthesized.content()).isEqualTo("charged-100");
        assertThat(synthesized.metadata()).containsEntry("dedupHit", true);
        assertThat(repairEvents).extracting(DanglingCallRepairer.RepairEvent::action)
                .containsExactly("dedup-hit");
        assertThat(sessionEvents).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("dedup-hit");
            assertThat(e.payload()).containsEntry("toolName", "charge");
        });
    }

    @Test
    void dedupMissKeepsCurrentBehavior_idempotentReplays_nonIdempotentSynthesizesInterrupted() {
        AtomicInteger idempotentCalls = new AtomicInteger();
        AtomicInteger sideEffectCalls = new AtomicInteger();
        var repairer = repairer(
                Map.of("read_file", tool("read_file", idempotentCalls, "file-content"),
                        "charge", tool("charge", sideEffectCalls, "charged")),
                Set.of("read_file"), Map.of());

        List<BuzhouMessage> repaired = repairer.repair(SESSION, List.of(
                danglingAssistant("tc-1", "read_file", "{}"),
                danglingAssistant("tc-2", "charge", "{}")));

        // 未命中：幂等工具维持重执行；非幂等维持合成交断结果
        assertThat(idempotentCalls).hasValue(1);
        assertThat(sideEffectCalls).hasValue(0);
        assertThat(repaired.stream().filter(m -> "tc-2".equals(m.toolCallId())))
                .allSatisfy(m -> assertThat(m.content()).isEqualTo(DanglingCallRepairer.INTERRUPTED_RESULT));
    }

    @Test
    void dedupMissOnPendingRecordDoesNotReuse() {
        // pending（前次执行未完成、结果未知）不算命中：不合成、不重执行非幂等工具
        recorder.reserve(SESSION, IdempotencyKeys.defaultKey("charge", "tc-1"));
        var repairer = repairer(Map.of(), Set.of(), Map.of());

        List<BuzhouMessage> repaired = repairer.repair(SESSION, List.of(danglingAssistant("tc-1", "charge", "{}")));

        assertThat(repaired.stream().filter(m -> "tc-1".equals(m.toolCallId())))
                .allSatisfy(m -> assertThat(m.content()).isEqualTo(DanglingCallRepairer.INTERRUPTED_RESULT));
    }

    @Test
    void businessKeyExtractorDrivesReplayDedupLookup() {
        // 业务覆盖键：重放路径同样按提取器派生键查去重记录
        recorder.fill(SESSION, IdempotencyKeys.businessKey("charge", "ORD-1"), "charged-ORD-1");
        var repairer = repairer(Map.of(), Set.of(),
                Map.of("charge", (IdempotencyKeyExtractor) (input, ctx) -> "ORD-1"));

        List<BuzhouMessage> repaired = repairer.repair(SESSION,
                List.of(danglingAssistant("tc-9", "charge", "{\"orderId\":\"ORD-1\"}")));

        assertThat(repaired.get(1).content()).isEqualTo("charged-ORD-1");
        assertThat(sessionEvents).anySatisfy(e -> assertThat(e.payload())
                .containsEntry("key", IdempotencyKeys.businessKey("charge", "ORD-1")));
    }

    @Test
    void nullExtractorResultFallsBackToDefaultKey() {
        recorder.fill(SESSION, IdempotencyKeys.defaultKey("charge", "tc-1"), "charged-default");
        var repairer = repairer(Map.of(), Set.of(),
                Map.of("charge", (IdempotencyKeyExtractor) (input, ctx) -> null));

        List<BuzhouMessage> repaired = repairer.repair(SESSION, List.of(danglingAssistant("tc-1", "charge", "{}")));

        assertThat(repaired.get(1).content()).isEqualTo("charged-default");
    }
}
