package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1428 / T2158：工具目录重名审计——重名组显形（名字典序）、
 * 三路同名计数、目录健康零组、空清单哨兵。
 */
class ToolCatalogDuplicateAuditTest {

    @Test
    void emptyListYieldsHealthyReport() {
        var r = ToolCatalogDuplicateAudit.analyze(List.of());
        assertThat(r.totalTools()).isZero();
        assertThat(r.distinctTools()).isZero();
        assertThat(r.duplicateCount()).isZero();
    }

    @Test
    void uniqueCatalogIsHealthy() {
        var r = ToolCatalogDuplicateAudit.analyze(List.of("read_file", "write_file", "search"));
        assertThat(r.totalTools()).isEqualTo(3);
        assertThat(r.distinctTools()).isEqualTo(3);
        assertThat(r.duplicateCount()).isZero();
    }

    @Test
    void duplicateGroupsListedWithCounts() {
        var r = ToolCatalogDuplicateAudit.analyze(List.of(
                "read_file", "read_file", "search", "write_file"));
        assertThat(r.totalTools()).isEqualTo(4);
        assertThat(r.distinctTools()).isEqualTo(3);
        assertThat(r.duplicates()).hasSize(1);
        assertThat(r.duplicates().get(0).toolName()).isEqualTo("read_file");
        assertThat(r.duplicates().get(0).count()).isEqualTo(2);
    }

    @Test
    void multipleDuplicatesSortedByName() {
        var r = ToolCatalogDuplicateAudit.analyze(List.of(
                "z_tool", "z_tool", "a_tool", "a_tool", "a_tool"));
        assertThat(r.duplicateCount()).isEqualTo(2);
        // 名字典序：a_tool 组在前
        assertThat(r.duplicates().get(0).toolName()).isEqualTo("a_tool");
        assertThat(r.duplicates().get(0).count()).isEqualTo(3);
        assertThat(r.duplicates().get(1).toolName()).isEqualTo("z_tool");
    }

    @Test
    void mcpAndLocalCollisionScenario() {
        // 真实场景：本地工具与 MCP server 工具同名（HashMap 静默遮蔽源）
        var r = ToolCatalogDuplicateAudit.analyze(List.of(
                "local_read_file", "mcp_read_file", "read_file", "read_file"));
        assertThat(r.duplicateCount()).isEqualTo(1);
        assertThat(r.duplicates().get(0).count()).isEqualTo(2);
    }
}
