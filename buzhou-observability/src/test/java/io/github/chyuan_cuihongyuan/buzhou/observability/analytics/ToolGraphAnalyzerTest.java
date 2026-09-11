package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 519 / T789–T790：工具调用图谱——同轮相邻有向边计数、乱序输入
 * startedAt 排序、错误 span 计红、非 TOOL 忽略、跨轮不连边、store 重载。
 */
class ToolGraphAnalyzerTest {

    private static SpanRecord span(String kind, String name, String session, int turn,
            String status, long startMillis) {
        Instant start = Instant.ofEpochMilli(startMillis);
        return new SpanRecord("sp-" + startMillis + kind + name, null, session, turn,
                kind, name, start, start.plusMillis(10), status, Map.of());
    }

    @Test
    void consecutiveToolSpansWithinTurnFormDirectedEdges() {
        var report = ToolGraphAnalyzer.analyze(List.of(
                span("TOOL", "search", "s1", 1, "OK", 1000),
                span("TOOL", "fetch", "s1", 1, "OK", 2000),
                span("TOOL", "reply", "s1", 1, "OK", 3000)));
        // 同值字典序稳定序：fetch→reply < search→fetch
        assertThat(report.edges()).containsExactly(
                new ToolGraphAnalyzer.Edge("fetch", "reply", 1),
                new ToolGraphAnalyzer.Edge("search", "fetch", 1));
        assertThat(report.tools()).hasSize(3);
        assertThat(report.tools().get(0).calls()).isEqualTo(1);
        assertThat(report.tools().get(0).errorRate()).isZero();
    }

    @Test
    void unsortedInputSortedByStartedAtAndNonToolIgnored() {
        var report = ToolGraphAnalyzer.analyze(List.of(
                span("TOOL", "fetch", "s1", 1, "OK", 2000),
                span("TURN", "turn", "s1", 1, "OK", 500),
                span("TOOL", "search", "s1", 1, "OK", 1000)));
        assertThat(report.edges()).containsExactly(
                new ToolGraphAnalyzer.Edge("search", "fetch", 1));
        assertThat(report.tools()).extracting(ToolGraphAnalyzer.ToolTotal::tool)
                .containsExactly("fetch", "search"); // calls 同值——字典序稳定
    }

    @Test
    void crossTurnNotConnectedAndErrorsCounted() {
        var report = ToolGraphAnalyzer.analyze(List.of(
                span("TOOL", "search", "s1", 1, "OK", 1000),
                span("TOOL", "search", "s1", 2, "ERROR", 2000),
                span("TOOL", "fetch", "s1", 2, "error", 3000)));
        // 跨轮不连边（turn1 search → turn2 search 无边）；turn2 内 search→fetch 有边
        assertThat(report.edges()).containsExactly(
                new ToolGraphAnalyzer.Edge("search", "fetch", 1));
        var search = report.tools().stream()
                .filter(t -> t.tool().equals("search")).findFirst().orElseThrow();
        assertThat(search.calls()).isEqualTo(2);
        assertThat(search.errors()).isEqualTo(1); // 大小写不敏感判红
        assertThat(search.errorRate()).isEqualTo(0.5);
    }

    @Test
    void storeOverloadReadsSingleSession() {
        ObservabilityStore store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou
                .inMemoryStores().observabilityStore();
        store.saveSpans(List.of(
                span("TOOL", "search", "s9", 1, "OK", 1000),
                span("TOOL", "fetch", "s9", 1, "OK", 2000)));
        var report = ToolGraphAnalyzer.analyze(store, "s9");
        assertThat(report.edges()).containsExactly(
                new ToolGraphAnalyzer.Edge("search", "fetch", 1));
    }

    @Test
    void nullSpansFailFast() {
        List<SpanRecord> nullList = null;
        assertThatThrownBy(() -> ToolGraphAnalyzer.analyze(nullList))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
