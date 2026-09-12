package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具调用图谱环检测测试（spec 717 / T985–T986 / impl 520）：双节点环/自环
 * 检出、锚去重不双报、链/菱形零误报、封顶确定、空图空表。
 */
class ToolGraphCyclesTest {

    private static ToolGraphAnalyzer.ToolGraphReport report(ToolGraphAnalyzer.Edge... edges) {
        return new ToolGraphAnalyzer.ToolGraphReport(List.of(edges), List.of());
    }

    @Test
    void twoNodeCycleDetectedOnce() {
        var report = report(
                new ToolGraphAnalyzer.Edge("a", "b", 5),
                new ToolGraphAnalyzer.Edge("b", "a", 3));

        List<List<String>> cycles = ToolGraphAnalyzer.cycles(report);

        assertThat(cycles).containsExactly(List.of("a", "b")); // B→A→B 不另报（锚去重）
    }

    @Test
    void selfLoopDetected() {
        var report = report(new ToolGraphAnalyzer.Edge("a", "a", 2));

        assertThat(ToolGraphAnalyzer.cycles(report)).containsExactly(List.of("a"));
    }

    @Test
    void acyclicChainAndDiamondHaveNoCycles() {
        var chain = report(
                new ToolGraphAnalyzer.Edge("a", "b", 1),
                new ToolGraphAnalyzer.Edge("b", "c", 1));
        var diamond = report(
                new ToolGraphAnalyzer.Edge("a", "b", 1),
                new ToolGraphAnalyzer.Edge("a", "c", 1),
                new ToolGraphAnalyzer.Edge("b", "d", 1),
                new ToolGraphAnalyzer.Edge("c", "d", 1));

        assertThat(ToolGraphAnalyzer.cycles(chain)).isEmpty();
        assertThat(ToolGraphAnalyzer.cycles(diamond)).isEmpty();
    }

    @Test
    void emptyReportYieldsEmptyCycles() {
        var empty = new ToolGraphAnalyzer.ToolGraphReport(List.of(), List.of());

        assertThat(ToolGraphAnalyzer.cycles(empty)).isEmpty();
    }

    @Test
    void capIsDeterministic() {
        // 4 节点全连双向 = 大量初等环（A→B→A、A→B→C→A、…）
        var report = report(
                new ToolGraphAnalyzer.Edge("a", "b", 1), new ToolGraphAnalyzer.Edge("b", "a", 1),
                new ToolGraphAnalyzer.Edge("a", "c", 1), new ToolGraphAnalyzer.Edge("c", "a", 1),
                new ToolGraphAnalyzer.Edge("a", "d", 1), new ToolGraphAnalyzer.Edge("d", "a", 1),
                new ToolGraphAnalyzer.Edge("b", "c", 1), new ToolGraphAnalyzer.Edge("c", "b", 1),
                new ToolGraphAnalyzer.Edge("b", "d", 1), new ToolGraphAnalyzer.Edge("d", "b", 1),
                new ToolGraphAnalyzer.Edge("c", "d", 1), new ToolGraphAnalyzer.Edge("d", "c", 1));

        List<List<String>> first = ToolGraphAnalyzer.cycles(report);
        List<List<String>> second = ToolGraphAnalyzer.cycles(report);

        assertThat(first).hasSize(ToolGraphAnalyzer.MAX_CYCLES); // 有界封顶
        assertThat(first).isEqualTo(second);                     // 两次枚举结果确定
        assertThat(first).isSortedAccordingTo((x, y) -> {
            int byLen = Integer.compare(x.size(), y.size());
            return byLen != 0 ? byLen : String.join("→", x).compareTo(String.join("→", y));
        });
    }

    @Test
    void threeNodeCycleWithTail() {
        var report = report(
                new ToolGraphAnalyzer.Edge("root", "a", 1),
                new ToolGraphAnalyzer.Edge("a", "b", 1),
                new ToolGraphAnalyzer.Edge("b", "c", 1),
                new ToolGraphAnalyzer.Edge("c", "a", 1));

        List<List<String>> cycles = ToolGraphAnalyzer.cycles(report);

        assertThat(cycles).containsExactly(List.of("a", "b", "c")); // root 不入环
    }
}
