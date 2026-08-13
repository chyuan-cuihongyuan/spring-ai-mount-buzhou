package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21 hot-tail/cold-storage 两级保留 + T22 per-tool durable override（docs/spec/11 spill）：
 * 近期 N 条工具结果全量内联、旧结果溢出为自描述占位符（回读可取真实切片）；
 * durable 声明永不溢出；keep-inline 数量与大小预算可配。
 */
class HotTailViewProcessorTest {

    private static final int THRESHOLD = 100;

    @TempDir
    Path rootDir;

    private final SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();

    private HotTailViewProcessor processor(DiskSpillStore store, int keepInline, long maxInlineChars,
                                           Map<String, Object> toolPolicies) {
        return new HotTailViewProcessor(new SpillService(store, 64, 3), keepInline, maxInlineChars,
                store::dataPathOf, registry, toolPolicies, THRESHOLD);
    }

    private BuzhouMessage toolMessage(String sessionId, String callId, String toolName, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 1, Role.TOOL,
                content, List.of(), callId, null, null, Map.of("toolName", toolName), Instant.now());
    }

    private BuzhouMessage userMessage(String sessionId, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void recentToolResultsStayInlineOldOnesSpillToSelfDescribingHandle() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        HotTailViewProcessor processor = processor(store, 2, 0, Map.of());
        String big = "old-detail-" + "x".repeat(THRESHOLD);
        List<BuzhouMessage> stored = List.of(
                userMessage("s1", "u1"),
                toolMessage("s1", "tc-1", "db_query", big),
                toolMessage("s1", "tc-2", "db_query", "recent-2"),
                toolMessage("s1", "tc-3", "db_query", "recent-3"));

        List<BuzhouMessage> view = processor.process("s1", stored, 1);

        // 近期 2 条全量内联（零损失）
        assertThat(view.get(2).content()).isEqualTo("recent-2");
        assertThat(view.get(3).content()).isEqualTo("recent-3");
        // 旧的大结果替换为自描述占位符（T20 格式），原文主体不再内联（仅余预览）
        assertThat(view.get(1).content())
                .contains("句柄：spill://" + HotTailViewProcessor.VIEW_AGENT + "/s1/tc-1")
                .contains("read_range")
                .doesNotContain(big);
        // 回读拿到真实切片（非编造）
        String readBack = store.readRange(
                SpillUri.parse("spill://" + HotTailViewProcessor.VIEW_AGENT + "/s1/tc-1"),
                RangeReadRequest.bytes(0, 12)).content();
        assertThat(readBack).startsWith("old-detail-");
        // 幂等：二次处理不再重复溢出
        List<BuzhouMessage> again = processor.process("s1", view, 1);
        assertThat(again.get(1).content()).isEqualTo(view.get(1).content());
    }

    @Test
    void durableDeclarationNeverSpillsEvenWhenAged() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        HotTailViewProcessor processor = processor(store, 1, 0,
                Map.of("db_schema", Map.of("spillNeverOffload", true)));
        String schema = "CREATE TABLE orders (" + "x".repeat(THRESHOLD * 2) + ");";
        List<BuzhouMessage> stored = List.of(
                userMessage("s1", "u1"),
                toolMessage("s1", "tc-schema", "db_schema", schema),
                toolMessage("s1", "tc-q", "db_query", "recent"));

        List<BuzhouMessage> view = processor.process("s1", stored, 1);

        // 声明 durable 的大输出保持全量内联（不溢出/不截断）
        assertThat(view.get(1).content()).isEqualTo(schema);
        // 未声明工具走默认阈值
        assertThat(view.get(2).content()).isEqualTo("recent");
    }

    @Test
    void perToolThresholdTokensAppliesToAgedResults() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        // mcp_big：5 token × 4 = 20 字符即溢出
        HotTailViewProcessor processor = processor(store, 1, 0,
                Map.of("mcp_big", Map.of("spillThresholdTokens", 5)));
        List<BuzhouMessage> stored = List.of(
                toolMessage("s1", "tc-old", "mcp_big", "y".repeat(30)),
                toolMessage("s1", "tc-new", "mcp_big", "recent"));

        List<BuzhouMessage> view = processor.process("s1", stored, 1);

        assertThat(view.get(0).content()).contains("spill://");
        assertThat(view.get(1).content()).isEqualTo("recent");
    }

    @Test
    void inlineCharBudgetSpillsOldestBeyondBudget() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        // keep-inline 3 条，但内联总预算 150 字符 → 超预算的最旧两条仍被溢出，最新的保留
        HotTailViewProcessor processor = processor(store, 3, 150, Map.of());
        List<BuzhouMessage> stored = List.of(
                toolMessage("s1", "tc-a", "t", "a".repeat(THRESHOLD)),
                toolMessage("s1", "tc-b", "t", "b".repeat(THRESHOLD)),
                toolMessage("s1", "tc-c", "t", "recent-c"));

        List<BuzhouMessage> view = processor.process("s1", stored, 1);

        assertThat(view.get(0).content()).contains("spill://");
        assertThat(view.get(1).content()).contains("spill://");
        // 预算内保留最新的
        assertThat(view.get(2).content()).isEqualTo("recent-c");
    }

    @Test
    void fewerToolResultsThanKeepInlineAreUntouched() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        HotTailViewProcessor processor = processor(store, 5, 0, Map.of());
        List<BuzhouMessage> stored = new ArrayList<>(List.of(
                toolMessage("s1", "tc-1", "t", "x".repeat(THRESHOLD)),
                toolMessage("s1", "tc-2", "t", "short")));

        List<BuzhouMessage> view = processor.process("s1", stored, 1);

        assertThat(view).isEqualTo(stored); // hot-tail 全量内联，零处理
    }
}
