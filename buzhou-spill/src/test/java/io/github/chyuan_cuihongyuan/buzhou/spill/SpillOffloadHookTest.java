package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpillOffloadHookTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int THRESHOLD = 100;

    @TempDir
    Path rootDir;

    private final List<SessionEvent> events = new ArrayList<>();
    private DiskSpillStore store;
    private SessionReadOnlyRegistry registry;
    private SpillOffloadHook hook;

    private void setup(SpillStore spillStore, int defaultThreshold, Map<String, Object> toolPolicies) {
        store = spillStore instanceof DiskSpillStore d ? d : null;
        registry = new SessionReadOnlyRegistry();
        SpillService service = new SpillService(spillStore, 64, 3);
        hook = new SpillOffloadHook(service, registry,
                uri -> store != null ? store.dataPathOf(uri) : rootDir.resolve("missing"),
                defaultThreshold, toolPolicies);
    }

    private ToolCallContext ctx(String sessionId, String toolCallId, String toolName, Object result) {
        HookEnvironment env = new HookEnvironment(sessionId, "agent", new InMemorySessionStateStore());
        env.bindEventPublisher(events::add);
        DefaultToolCallContext ctx = new DefaultToolCallContext(env, toolCallId, toolName, Map.of());
        ctx.markExecuted(result, null);
        return ctx;
    }

    @Test
    void orderIsBuiltinAfterToolSlot() {
        setup(new DiskSpillStore(rootDir), THRESHOLD, Map.of());
        assertThat(hook.order()).isEqualTo(100);
    }

    @Test
    void belowThresholdPassesThrough() {
        setup(new DiskSpillStore(rootDir), THRESHOLD, Map.of());
        ToolCallContext ctx = ctx("s1", "tc-1", "read_file", "short result");

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isSameAs(HookResult.CONTINUE);
        assertThat(ctx.result()).isEqualTo("short result");
    }

    @Test
    void aboveThresholdReplacesWithReferenceHandle() {
        DiskSpillStore disk = new DiskSpillStore(rootDir);
        setup(disk, THRESHOLD, Map.of());
        String big = "x".repeat(500);
        ToolCallContext ctx = ctx("s1", "tc-1", "read_file", big);

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        String handle = (String) ((HookResult.Replace) result).payload();
        assertThat(handle).contains("spill://agent/s1/tc-1").contains("read_range");
        assertThat(handle).doesNotContain(big);
        assertThat(disk.load(new SpillUri("agent", "s1", "tc-1"))).contains(big);
        assertThat(registry.isReadOnly("s1", disk.dataPathOf(new SpillUri("agent", "s1", "tc-1"))))
                .isTrue();
    }

    @Test
    void perToolThresholdOverrideViaToolPolicies() {
        DiskSpillStore disk = new DiskSpillStore(rootDir);
        setup(disk, THRESHOLD, Map.of("mcp_*", Map.of("spillThresholdChars", 10)));
        ToolCallContext ctx = ctx("s1", "tc-1", "mcp_query", "y".repeat(50));

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        assertThat(disk.exists(new SpillUri("agent", "s1", "tc-1"))).isTrue();
    }

    @Test
    void arrayResultJudgedPerItem() throws Exception {
        DiskSpillStore disk = new DiskSpillStore(rootDir);
        setup(disk, THRESHOLD, Map.of());
        String big = "z".repeat(300);
        ArrayNode array = MAPPER.createArrayNode();
        array.add(big);
        array.add("tiny");
        ToolCallContext ctx = ctx("s1", "tc-9", "batch_query", MAPPER.writeValueAsString(array));

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        JsonNode replaced = MAPPER.readTree((String) ((HookResult.Replace) result).payload());
        assertThat(replaced.get(0).asText()).contains("spill://agent/s1/tc-9-0");
        assertThat(replaced.get(0).asText()).doesNotContain(big);
        assertThat(replaced.get(1).asText()).isEqualTo("tiny");
        assertThat(disk.load(new SpillUri("agent", "s1", "tc-9-0"))).contains(big);
        assertThat(disk.exists(new SpillUri("agent", "s1", "tc-9-1"))).isFalse();
    }

    @Test
    void storeFailureDegradesPassthroughAndEmitsWarningEvent() {
        setup(new FailingSpillStore(), THRESHOLD, Map.of());
        String big = "x".repeat(500);
        ToolCallContext ctx = ctx("s1", "tc-1", "read_file", big);

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isSameAs(HookResult.CONTINUE);
        assertThat(ctx.result()).isEqualTo(big);
        assertThat(events).anyMatch(e -> e.type().equals("offload.degraded"));
    }

    @Test
    void arrayItemFailureDegradesOnlyThatItem() throws Exception {
        SpillStore selective = new DiskSpillStore(rootDir) {
            @Override
            public SpillHandle store(SpillEntry entry, int previewChars) {
                if (entry.content().contains("POISON")) {
                    throw new RuntimeException("simulated store failure");
                }
                return super.store(entry, previewChars);
            }
        };
        setup(selective, THRESHOLD, Map.of());
        String bad = "POISON" + "b".repeat(300);
        String good = "g".repeat(300);
        ArrayNode array = MAPPER.createArrayNode();
        array.add(bad);
        array.add(good);
        ToolCallContext ctx = ctx("s1", "tc-7", "batch_query", MAPPER.writeValueAsString(array));

        HookResult result = hook.afterTool(ctx);

        assertThat(result).isInstanceOf(HookResult.Replace.class);
        JsonNode replaced = MAPPER.readTree((String) ((HookResult.Replace) result).payload());
        assertThat(replaced.get(0).asText()).isEqualTo(bad);
        assertThat(replaced.get(1).asText()).contains("spill://agent/s1/tc-7-1");
        assertThat(events).anyMatch(e -> e.type().equals("offload.degraded"));
    }

    @Test
    void nullResultOrErrorPassesThrough() {
        setup(new DiskSpillStore(rootDir), THRESHOLD, Map.of());
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultToolCallContext nullCtx = new DefaultToolCallContext(env, "tc-1", "t", Map.of());
        DefaultToolCallContext errCtx = new DefaultToolCallContext(env, "tc-2", "t", Map.of());
        errCtx.markExecuted("失败", new RuntimeException("boom"));

        assertThat(hook.afterTool(nullCtx)).isSameAs(HookResult.CONTINUE);
        assertThat(hook.afterTool(errCtx)).isSameAs(HookResult.CONTINUE);
    }

    static class FailingSpillStore implements SpillStore {
        @Override
        public SpillHandle store(SpillEntry entry, int previewChars) {
            throw new RuntimeException("disk full");
        }

        @Override
        public Optional<String> load(SpillUri uri) {
            return Optional.empty();
        }

        @Override
        public RangeReadResult readRange(SpillUri uri, RangeReadRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markLinked(SpillUri uri) {
        }

        @Override
        public void delete(SpillUri uri) {
        }

        @Override
        public int deleteBySession(String agentName, String sessionId) {
            return 0;
        }

        @Override
        public int deleteExpired(Instant now, Duration ttl) {
            return 0;
        }

        @Override
        public boolean exists(SpillUri uri) {
            return false;
        }
    }
}
