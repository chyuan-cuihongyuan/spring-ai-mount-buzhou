package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1414 / T2130：Span 树拓扑——深度/扇出/孤儿/根计数、kind 直方排序、
 * 环防护收敛、空集合哨兵（Jaeger DAG 结构形状思想）。
 */
class SpanTreeTopologyTest {

    private static SpanRecord span(String id, String parent, String kind) {
        return new SpanRecord(id, parent, "s-t", 1, kind, id,
                Instant.parse("2026-09-14T10:00:00Z"),
                Instant.parse("2026-09-14T10:00:01Z"), "OK", Map.of());
    }

    @Test
    void emptyInputYieldsZeroTopology() {
        var t = SpanTreeTopology.analyze(List.of());
        assertThat(t.totalSpans()).isZero();
        assertThat(t.maxDepth()).isZero();
        assertThat(t.kindHistogram()).isEmpty();
    }

    @Test
    void depthAndFanoutMeasuredOnThreeLevelTree() {
        // SESSION ← TURN ← (MODEL_CALL, TOOL_CALL×3)
        List<SpanRecord> spans = List.of(
                span("sess", null, "SESSION"),
                span("turn", "sess", "TURN"),
                span("m1", "turn", "MODEL_CALL"),
                span("tc1", "turn", "TOOL_CALL"),
                span("tc2", "turn", "TOOL_CALL"),
                span("tc3", "turn", "TOOL_CALL"));
        var t = SpanTreeTopology.analyze(spans);
        assertThat(t.totalSpans()).isEqualTo(6);
        assertThat(t.rootCount()).isEqualTo(1);
        assertThat(t.orphanCount()).isZero();
        assertThat(t.maxDepth()).isEqualTo(3); // sess→turn→tc
        assertThat(t.maxFanout()).isEqualTo(4); // turn 有 4 子
        assertThat(t.kindHistogram().get("TOOL_CALL")).isEqualTo(3);
        // 直方数量降序
        var kinds = t.kindHistogram().values().stream().toList();
        assertThat(kinds).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void orphansCountedWhenParentMissing() {
        List<SpanRecord> spans = List.of(
                span("a", null, "SESSION"),
                span("ghost-child", "not-in-set", "TOOL_CALL"));
        var t = SpanTreeTopology.analyze(spans);
        assertThat(t.orphanCount()).isEqualTo(1);
        assertThat(t.rootCount()).isEqualTo(1);
        // 孤儿不计入深度（从根可达的链上没有它）
        assertThat(t.maxDepth()).isEqualTo(1);
    }

    @Test
    void parentCycleTerminatesInsteadOfStackOverflow() {
        // 互为父子——环防护：不无限递归
        List<SpanRecord> spans = List.of(
                span("x", "y", "A"),
                span("y", "x", "B"),
                span("root", null, "SESSION"));
        var t = SpanTreeTopology.analyze(spans);
        assertThat(t.totalSpans()).isEqualTo(3);
        assertThat(t.maxDepth()).isEqualTo(1); // 只有 root 是根；环不计深不炸栈
    }

    @Test
    void multipleRootsAndKindTieBreakByName() {
        List<SpanRecord> spans = List.of(
                span("r1", null, "TURN"),
                span("r2", null, "SESSION"),
                span("r3", null, "SESSION"));
        var t = SpanTreeTopology.analyze(spans);
        assertThat(t.rootCount()).isEqualTo(3);
        // 同数量按名典序：SESSION 在 TURN 前
        var names = t.kindHistogram().keySet().stream().toList();
        assertThat(names).containsExactly("SESSION", "TURN");
    }
}
