package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolGraphAnalyzer 边缘分支补测（K 会话 R14 / spec 1213 / T1835——R7 逐类分支数据精定制导）：
 * cycles(null) fail-fast、零计数边不入邻接、边 count 同值时 from 字典序 tie-break、
 * kind=null span 忽略、timings 的 null startedAt 计 0 与 null spanId 不入父子索引。
 * 主口径由既有 ToolGraphAnalyzerTest / ToolGraphCyclesTest / ToolFlameTimingTest 覆盖。
 */
class ToolGraphAnalyzerEdgeTest {

    private static SpanRecord span(String spanId, String parent, String kind, String name,
                                   String session, int turn, long startMillis, long endMillis, String status) {
        return new SpanRecord(spanId, parent, session, turn, kind, name,
                Instant.ofEpochMilli(startMillis),
                endMillis < 0 ? null : Instant.ofEpochMilli(endMillis),
                status, Map.of());
    }

    @Test
    void cyclesNullReportFailsFast() {
        ToolGraphAnalyzer.ToolGraphReport nullReport = null;
        assertThatThrownBy(() -> ToolGraphAnalyzer.cycles(nullReport))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroCountEdgesAreSkippedFromAdjacency() {
        // 零计数边不构成环：A→B 与 B→A 中 B→A 计 0 → 图无环
        var report = new ToolGraphAnalyzer.ToolGraphReport(
                List.of(new ToolGraphAnalyzer.Edge("a", "b", 2),
                        new ToolGraphAnalyzer.Edge("b", "a", 0)),
                List.of());
        assertThat(ToolGraphAnalyzer.cycles(report)).isEmpty();
    }

    @Test
    void equalCountEdgesTieBreakByFromThenTo() {
        // 两边 count 同值（各 1）→ from 字典序 tie-break：fetch 在 search 前
        var report = ToolGraphAnalyzer.analyze(List.of(
                span("s1", null, "TOOL", "search", "sess", 1, 1000, 1001, "OK"),
                span("s2", null, "TOOL", "fetch", "sess", 1, 1002, 1003, "OK"),
                span("s3", null, "TOOL", "reply", "sess", 1, 1004, 1005, "OK")));

        assertThat(report.edges()).containsExactly(
                new ToolGraphAnalyzer.Edge("fetch", "reply", 1),
                new ToolGraphAnalyzer.Edge("search", "fetch", 1));
    }

    @Test
    void nullKindSpanIsIgnored() {
        var report = ToolGraphAnalyzer.analyze(List.of(
                span("s0", null, null, "no-kind", "sess", 1, 1000, 1001, "OK"),
                span("s1", null, "tool", "lower-tool", "sess", 1, 1002, 1003, "OK")));

        // kind=null 忽略；kind 大小写不敏感（"tool" 计入）
        assertThat(report.tools()).hasSize(1);
        assertThat(report.tools().get(0).tool()).isEqualTo("lower-tool");
    }

    @Test
    void timingsNullStartedAtCountsZeroAndNullSpanIdSkipsHierarchy() {
        // startedAt=null → self 计 0；spanId=null → 不入父子索引（作为根计自身）
        var report = ToolGraphAnalyzer.timings(List.of(
                new SpanRecord(null, null, "sess", 1, "TOOL", "no-id",
                        null, Instant.ofEpochMilli(1005), "OK", Map.of())));

        assertThat(report).hasSize(1);
        assertThat(report.get(0).totalSelfMs()).isZero();
        assertThat(report.get(0).totalCumulativeMs()).isZero();
    }

    @Test
    void negativeDurationClampedToZero() {
        // endedAt < startedAt（时钟偏移）→ 夹 0
        var report = ToolGraphAnalyzer.timings(List.of(
                span("s1", null, "TOOL", "backwards", "sess", 1, 2000, 1000, "OK")));

        assertThat(report.get(0).totalSelfMs()).isZero();
    }
}
