package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
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

class DanglingCallRepairerTest {

    private final List<DanglingCallRepairer.RepairEvent> events = new ArrayList<>();

    private BuzhouMessage assistant(String sessionId, int seq, String content, ToolCallRecord... calls) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, seq, Role.ASSISTANT,
                content, List.of(calls), null, null, null, Map.of(), Instant.now());
    }

    private BuzhouMessage toolResult(String sessionId, int seq, String toolCallId, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, seq, Role.TOOL,
                content, List.of(), toolCallId, null, null, Map.of(), Instant.now());
    }

    private DanglingCallRepairer repairer(Map<String, ToolCallback> tools, Set<String> idempotent) {
        return new DanglingCallRepairer(tools, idempotent,
                (sid, event) -> events.add(event));
    }

    @Test
    void fullyDanglingBlankAssistantIsDropped() {
        var repairer = repairer(Map.of(), Set.of());
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "", new ToolCallRecord("tc-1", "query", "{}")));

        List<BuzhouMessage> repaired = repairer.repair("s", stored);

        assertThat(repaired).isEmpty();
        assertThat(events).extracting(DanglingCallRepairer.RepairEvent::action)
                .containsExactly("dropped");
    }

    @Test
    void fullyDanglingAssistantWithTextIsDemoted() {
        var repairer = repairer(Map.of(), Set.of());
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "我来查一下", new ToolCallRecord("tc-1", "query", "{}")));

        List<BuzhouMessage> repaired = repairer.repair("s", stored);

        assertThat(repaired).hasSize(1);
        assertThat(repaired.getFirst().toolCalls()).isEmpty();
        assertThat(repaired.getFirst().content()).isEqualTo("我来查一下");
        assertThat(events).extracting(DanglingCallRepairer.RepairEvent::action)
                .containsExactly("demoted");
    }

    @Test
    void partiallyDanglingSynthesizesInterruptedResult() {
        var repairer = repairer(Map.of(), Set.of());
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "", new ToolCallRecord("tc-1", "a", "{}"),
                        new ToolCallRecord("tc-2", "b", "{}")),
                toolResult("s", 2, "tc-1", "ok"));

        List<BuzhouMessage> repaired = repairer.repair("s", stored);

        assertThat(repaired).hasSize(3);
        assertThat(repaired.get(2).toolCallId()).isEqualTo("tc-1");
        BuzhouMessage synthetic = repaired.get(1);
        assertThat(synthetic.toolCallId()).isEqualTo("tc-2");
        assertThat(synthetic.content()).isEqualTo(DanglingCallRepairer.INTERRUPTED_RESULT);
        assertThat(synthetic.metadata()).containsEntry("synthetic", true);
        assertThat(events).extracting(DanglingCallRepairer.RepairEvent::action)
                .containsExactly("synthesized");
    }

    @Test
    void idempotentToolIsReplayedOnceBeforeRepair() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("query").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return "{\"real\":true}";
            }
        };
        var repairer = repairer(Map.of("query", flaky), Set.of("query"));
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "", new ToolCallRecord("tc-1", "query", "{}")));

        List<BuzhouMessage> repaired = repairer.repair("s", stored);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(repaired).hasSize(2);
        assertThat(repaired.get(1).content()).isEqualTo("{\"real\":true}");
        assertThat(repaired.get(1).metadata()).containsEntry("replayed", true);
        assertThat(events).extracting(DanglingCallRepairer.RepairEvent::action)
                .containsExactly("replayed");
    }

    @Test
    void nonIdempotentToolIsNotReplayed() {
        AtomicInteger calls = new AtomicInteger();
        ToolCallback dangerous = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("write").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return "done";
            }
        };
        var repairer = repairer(Map.of("write", dangerous), Set.of());
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "", new ToolCallRecord("tc-1", "write", "{}")));

        repairer.repair("s", stored);

        assertThat(calls.get()).isZero();
    }

    @Test
    void completedCallsAreUntouched() {
        var repairer = repairer(Map.of(), Set.of());
        List<BuzhouMessage> stored = List.of(
                assistant("s", 1, "", new ToolCallRecord("tc-1", "a", "{}")),
                toolResult("s", 2, "tc-1", "ok"));

        List<BuzhouMessage> repaired = repairer.repair("s", stored);

        assertThat(repaired).isEqualTo(stored);
        assertThat(events).isEmpty();
    }
}
