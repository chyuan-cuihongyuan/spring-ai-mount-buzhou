package io.github.chyuan_cuihongyuan.buzhou.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 706 / T1012–T1013：MCP 工具目录差异——三类变更+危险翻转标记、
 * server 三态、聚合计数、null fail-fast。
 */
class McpDirectoryDiffTest {

    private static McpToolHints hints(String title, boolean readOnly, boolean destructive) {
        return new McpToolHints(title, readOnly, destructive, false, false);
    }

    @Test
    void addedRemovedAndRiskyFlipAreReported() {
        Map<String, Map<String, McpToolHints>> baseline = Map.of(
                "fs", Map.of(
                        "read", hints("读文件", true, false),
                        "write", hints("写文件", false, false),
                        "legacy", hints("旧工具", true, false)));
        Map<String, Map<String, McpToolHints>> current = Map.of(
                "fs", Map.of(
                        "read", hints("读文件", true, false),          // 未变
                        "write", hints("写文件", true, false),         // 翻转但安全方向（readOnly→true）
                        "purge", hints("清库", false, true)));         // 新增且危险画像
        McpDirectoryDiff.Report report = McpDirectoryDiff.diff(baseline, current);
        assertThat(report.servers()).hasSize(1);
        assertThat(report.servers().get(0).status()).isEqualTo(McpDirectoryDiff.SyncStatus.DRIFTED);
        assertThat(report.added()).isEqualTo(1);
        assertThat(report.removed()).isEqualTo(1);
        assertThat(report.hintChanged()).isEqualTo(1);
        // 新增 destructive 画像工具 = risky；readOnly true→false 才是翻转 risky
        assertThat(report.risky()).isEqualTo(1);
        assertThat(report.servers().get(0).changes())
                .extracting(McpDirectoryDiff.ToolChange::kind)
                .containsExactly(McpDirectoryDiff.ChangeKind.REMOVED,      // legacy（字典序最前）
                        McpDirectoryDiff.ChangeKind.ADDED,                 // purge
                        McpDirectoryDiff.ChangeKind.HINT_CHANGED);         // write（read 未变）
    }

    @Test
    void readOnlyToFalseFlipIsRisky() {
        Map<String, Map<String, McpToolHints>> baseline = Map.of(
                "db", Map.of("query", hints("查询", true, false)));
        Map<String, Map<String, McpToolHints>> current = Map.of(
                "db", Map.of("query", hints("查询", false, true)));
        McpDirectoryDiff.Report report = McpDirectoryDiff.diff(baseline, current);
        assertThat(report.hintChanged()).isEqualTo(1);
        assertThat(report.risky()).isEqualTo(1);
        assertThat(report.servers().get(0).changes().get(0).detail())
                .contains("readOnlyHint: true→false")
                .contains("destructiveHint: false→true");
    }

    @Test
    void serverNewGoneAndInSyncStatuses() {
        Map<String, Map<String, McpToolHints>> baseline = Map.of(
                "old", Map.of("t", hints("", true, false)),
                "same", Map.of("t", hints("", true, false)));
        Map<String, Map<String, McpToolHints>> current = Map.of(
                "same", Map.of("t", hints("", true, false)),
                "new", Map.of("t", hints("", true, false)));
        McpDirectoryDiff.Report report = McpDirectoryDiff.diff(baseline, current);
        assertThat(report.servers()).extracting(McpDirectoryDiff.ServerDiff::server)
                .containsExactly("new", "old", "same"); // 字典序
        assertThat(report.servers()).extracting(McpDirectoryDiff.ServerDiff::status)
                .containsExactly(McpDirectoryDiff.SyncStatus.SERVER_NEW,
                        McpDirectoryDiff.SyncStatus.SERVER_GONE,
                        McpDirectoryDiff.SyncStatus.IN_SYNC);
        assertThat(report.added()).isZero();
        assertThat(report.removed()).isZero();
    }

    @Test
    void nullsFailFastAndEmptyVsEmptyIsEmpty() {
        assertThatThrownBy(() -> McpDirectoryDiff.diff(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> McpDirectoryDiff.diff(Map.of(), null))
                .isInstanceOf(NullPointerException.class);
        McpDirectoryDiff.Report empty = McpDirectoryDiff.diff(Map.of(), Map.of());
        assertThat(empty.servers()).isEmpty();
        assertThat(empty.risky()).isZero();
    }
}
