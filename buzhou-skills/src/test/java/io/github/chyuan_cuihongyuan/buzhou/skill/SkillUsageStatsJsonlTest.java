package io.github.chyuan_cuihongyuan.buzhou.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 170 §B / T524：技能使用报表导出红队——与 topUsed 同序平铺；行独立解析；
 * 空表零行诚实；export→reset 窗口循环。
 */
class SkillUsageStatsJsonlTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exportsRowsInTopUsedOrder() throws Exception {
        SkillUsageStats stats = SkillUsageStats.create();
        stats.recordLoad("git-commit");
        stats.recordLoad("git-commit");
        stats.recordLoad("doc-arch");

        StringWriter out = new StringWriter();
        assertThat(SkillUsageStatsJsonl.export(stats, out)).isEqualTo(2);

        String[] rows = out.toString().split("\n", -1);
        JsonNode first = MAPPER.readTree(rows[0]);
        assertThat(first.get("skill").asText()).isEqualTo("git-commit");
        assertThat(first.get("loads").asLong()).isEqualTo(2L);
        JsonNode second = MAPPER.readTree(rows[1]);
        assertThat(second.get("skill").asText()).isEqualTo("doc-arch");
        assertThat(second.get("loads").asLong()).isEqualTo(1L);
    }

    @Test
    void emptyExportsZeroLinesAndWindowResets() throws Exception {
        SkillUsageStats stats = SkillUsageStats.create();
        StringWriter empty = new StringWriter();
        assertThat(SkillUsageStatsJsonl.export(stats, empty)).isZero();
        assertThat(empty.toString()).isEmpty();

        stats.recordLoad("x");
        stats.reset(); // 窗口切换
        StringWriter next = new StringWriter();
        assertThat(SkillUsageStatsJsonl.export(stats, next)).isZero();
    }
}
