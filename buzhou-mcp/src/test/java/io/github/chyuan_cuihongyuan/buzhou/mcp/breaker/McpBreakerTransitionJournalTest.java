package io.github.chyuan_cuihongyuan.buzhou.mcp.breaker;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 814 / T1130：MCP 断路器变迁台账回归——OPEN 变迁入账/恢复 CLOSED/
 * 聚合降序/环形挤老 dropped/null journal 原行为。
 */
class McpBreakerTransitionJournalTest {

    private static final Duration COOLDOWN = Duration.ofMillis(60);

    /** 可控成败的假工具（与 504 测试同形状）。 */
    static final class FlakyTool implements ToolCallback {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("tool_x").description("x")
                    .inputSchema("{\"type\":\"object\"}").build();
        }

        @Override
        public String call(String toolInput) {
            calls.incrementAndGet();
            if (failures.get() > 0) {
                failures.decrementAndGet();
                throw new IllegalStateException("server boom");
            }
            return "ok";
        }
    }

    @Test
    void recordsTripAndRecoverAcrossLifecycle() {
        McpBreakerTransitionJournal journal = new McpBreakerTransitionJournal();
        McpServerBreaker breaker = new McpServerBreaker(
                new ToolCircuitBreaker.Config(2, 50.0, COOLDOWN, 1), journal);
        FlakyTool tool = new FlakyTool();
        ToolCallback decorated = breaker.decorate("srv", tool);

        // 满窗全败 → OPEN（trip）
        tool.failures.set(2);
        assertThatThrownBy(() -> decorated.call("{}"));
        assertThatThrownBy(() -> decorated.call("{}"));
        assertThat(breaker.snapshot().get("srv").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN);

        // OPEN 快速失败（无状态变化——不重复入账）
        assertThatThrownBy(() -> decorated.call("{}"));
        assertThat(journal.snapshot().byServer().get(0).transitions()).isEqualTo(1);

        // 冷却耗尽 → 半开探测成功 → CLOSED（recover）
        try {
            Thread.sleep(COOLDOWN.toMillis() + 60);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        decorated.call("{}");

        McpBreakerTransitionJournal.Report report = journal.snapshot();
        assertThat(report.byServer().get(0).server()).isEqualTo("srv");
        assertThat(report.byServer().get(0).trips()).isEqualTo(1);
        assertThat(report.byServer().get(0).recovers()).isEqualTo(1);
        assertThat(report.byServer().get(0).transitions()).isEqualTo(2);
        assertThat(report.recent().get(0).to()).isEqualTo("CLOSED"); // 新→旧
        // 半开态瞬时不被采样（检测点在成败记录后）——直达路径 OPEN→CLOSED
        assertThat(report.recent().get(0).from()).isEqualTo("OPEN");
    }

    @Test
    void ringEvictionCountsDroppedAndIgnoresSameState() {
        McpBreakerTransitionJournal journal = new McpBreakerTransitionJournal();
        for (int i = 0; i < McpBreakerTransitionJournal.RING_CAPACITY + 4; i++) {
            journal.record("s", "CLOSED", "OPEN", i);
            journal.record("s", "OPEN", "CLOSED", i); // 成对避免同态忽略
        }
        McpBreakerTransitionJournal.Report report = journal.snapshot();
        assertThat(report.recent()).hasSize(McpBreakerTransitionJournal.RING_CAPACITY);
        assertThat(report.dropped()).isEqualTo(72); // 68 轮×2 条=136，环 64 → 挤 72
        // 同态变迁被忽略：record(s, OPEN, OPEN) 不入账
        int before = journal.snapshot().recent().size();
        journal.record("s", "OPEN", "OPEN", 999);
        assertThat(journal.snapshot().recent().size()).isEqualTo(before);
    }

    @Test
    void aggregateCapStopsNewKeysButRingContinues() {
        McpBreakerTransitionJournal journal = new McpBreakerTransitionJournal();
        for (int i = 0; i < McpBreakerTransitionJournal.AGGREGATE_CAP + 3; i++) {
            journal.record("srv" + i, "CLOSED", "OPEN", i);
        }
        McpBreakerTransitionJournal.Report report = journal.snapshot();
        assertThat(report.byServer()).hasSize(McpBreakerTransitionJournal.AGGREGATE_CAP);
        assertThat(report.recent()).hasSize(McpBreakerTransitionJournal.AGGREGATE_CAP + 3); // 明细全记
        assertThat(report.recent().get(0).server())
                .isEqualTo("srv" + (McpBreakerTransitionJournal.AGGREGATE_CAP + 2));
    }

    @Test
    void nullJournalKeepsOriginalBehavior() {
        McpServerBreaker breaker = new McpServerBreaker(
                new ToolCircuitBreaker.Config(2, 50.0, COOLDOWN, 1));
        FlakyTool tool = new FlakyTool();
        ToolCallback decorated = breaker.decorate("srv2", tool);
        tool.failures.set(2);
        assertThatThrownBy(() -> decorated.call("{}"));
        assertThatThrownBy(() -> decorated.call("{}"));
        assertThat(breaker.snapshot().get("srv2").state())
                .isEqualTo(ToolCircuitBreaker.State.OPEN); // 原语义不受影响
    }
}
