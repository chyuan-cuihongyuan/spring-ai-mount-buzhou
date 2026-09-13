package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-659 / spec 906：工具耗时火焰图数据面——父子树 self/cumulative 分解、
 * 父不在集合的根处理、环数据防护、RUNNING 计 0、稳定排序、既有面零回归。
 */
class ToolFlameTimingTest {

    private static SpanRecord span(String id, String parent, String tool, long millis) {
        Instant start = Instant.ofEpochMilli(1_000_000);
        return new SpanRecord(id, parent, "s1", 1, "TOOL", tool,
                start, start.plusMillis(millis), "OK", java.util.Map.of());
    }

    private static SpanRecord running(String id, String parent, String tool) {
        return new SpanRecord(id, parent, "s1", 1, "TOOL", tool,
                Instant.ofEpochMilli(1_000_000), null, "RUNNING", java.util.Map.of());
    }

    @Test
    void parentChildTreeDecomposesSelfAndCumulative() {
        // tree: search(100) → fetch(50) → parse(30)；search cumulative=180、self=100
        List<ToolGraphAnalyzer.ToolTimingProfile> profiles = ToolGraphAnalyzer.timings(List.of(
                span("s1", null, "search", 100),
                span("s2", "s1", "fetch", 50),
                span("s3", "s2", "parse", 30)));

        assertThat(profiles).hasSize(3);
        ToolGraphAnalyzer.ToolTimingProfile search = byTool(profiles, "search");
        assertThat(search.totalSelfMs()).isEqualTo(100);
        assertThat(search.totalCumulativeMs()).isEqualTo(180);
        assertThat(byTool(profiles, "fetch").totalCumulativeMs()).isEqualTo(80);
        assertThat(byTool(profiles, "parse").totalCumulativeMs()).isEqualTo(30);
        // 排序：self 降序 → search 在前
        assertThat(profiles.get(0).tool()).isEqualTo("search");
    }

    @Test
    void parentOutsideToolSetTreatedAsRoot() {
        // 父是 MODEL span（不在 TOOL 子集）→ 根，cumulative == self
        List<ToolGraphAnalyzer.ToolTimingProfile> profiles = ToolGraphAnalyzer.timings(List.of(
                new SpanRecord("m1", null, "s1", 1, "MODEL", "chat",
                        Instant.ofEpochMilli(1_000_000), Instant.ofEpochMilli(1_001_000),
                        "OK", java.util.Map.of()),
                span("t1", "m1", "search", 70)));

        assertThat(byTool(profiles, "search").totalSelfMs()).isEqualTo(70);
        assertThat(byTool(profiles, "search").totalCumulativeMs()).isEqualTo(70);
    }

    @Test
    void corruptedParentCycleDoesNotLoopForever() {
        // 数据损坏：a.parent=b、b.parent=a（互指环）——不死循环、断开记 0
        List<ToolGraphAnalyzer.ToolTimingProfile> profiles = ToolGraphAnalyzer.timings(List.of(
                span("a", "b", "toolA", 10),
                span("b", "a", "toolB", 20)));
        assertThat(profiles).hasSize(2);
        assertThat(byTool(profiles, "toolA").totalSelfMs()).isEqualTo(10);
        assertThat(byTool(profiles, "toolB").totalSelfMs()).isEqualTo(20);
    }

    @Test
    void runningSpanCountsZeroAndNullKindIgnored() {
        List<ToolGraphAnalyzer.ToolTimingProfile> profiles = ToolGraphAnalyzer.timings(List.of(
                running("r1", null, "hung"),
                new SpanRecord("x1", "r1", "s1", 1, null, "other",
                        Instant.ofEpochMilli(1_000_000), Instant.ofEpochMilli(1_001_000),
                        "OK", java.util.Map.of())));
        assertThat(byTool(profiles, "hung").totalSelfMs()).isZero();
        assertThat(byTool(profiles, "hung").totalCumulativeMs()).isZero();
        assertThat(profiles).hasSize(1); // kind=null 的非 TOOL span 被过滤
    }

    @Test
    void sortStableAndAggregatedAcrossSpans() {
        // 同工具两个独立 span 聚合：calls=2、self=40+60
        List<SpanRecord> spans = new ArrayList<>(List.of(
                span("p1", null, "db", 60),
                span("p2", null, "db", 40),
                span("q1", null, "api", 30)));
        List<ToolGraphAnalyzer.ToolTimingProfile> profiles = ToolGraphAnalyzer.timings(spans);
        assertThat(byTool(profiles, "db").calls()).isEqualTo(2);
        assertThat(byTool(profiles, "db").totalSelfMs()).isEqualTo(100);
        // 字典序 tie-break：交换输入顺序输出不变（稳定）
        List<ToolGraphAnalyzer.ToolTimingProfile> reversed = ToolGraphAnalyzer.timings(
                new ArrayList<>(List.of(span("q1", null, "api", 30),
                        span("p2", null, "db", 40), span("p1", null, "db", 60))));
        assertThat(reversed).isEqualTo(profiles);
    }

    @Test
    void nullSpansFailFast() {
        assertThatThrownBy(() -> ToolGraphAnalyzer.timings(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ToolGraphAnalyzer.ToolTimingProfile byTool(List<ToolGraphAnalyzer.ToolTimingProfile> profiles, String tool) {
        return profiles.stream().filter(p -> p.tool().equals(tool)).findFirst().orElseThrow();
    }
}
