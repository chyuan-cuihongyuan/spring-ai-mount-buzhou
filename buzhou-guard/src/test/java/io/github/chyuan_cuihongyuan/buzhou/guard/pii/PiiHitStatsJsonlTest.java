package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 166 §B / T519：PII 命中报表导出红队——一行一 JSON 与 top() 同序
 * （count 降序 + 名字典序）；行独立可解析；含自定义规则名（转义安全）；
 * 空表零行诚实；export→reset 循环语义。
 */
class PiiHitStatsJsonlTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exportsSortedRowsParsingIndependently() throws Exception {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.EMAIL);
        stats.record(PiiType.EMAIL);
        stats.record(PiiType.CN_PHONE);
        stats.recordCustom("PROJECT\"_CODE\n含换行"); // 转义纪律：引号/换行不撕行

        StringWriter out = new StringWriter();
        long lines = PiiHitStatsJsonl.export(stats, out);

        assertThat(lines).isEqualTo(3);
        String[] rows = out.toString().split("\n", -1);
        assertThat(rows).hasSize((int) lines + 1); // 末行换行空尾

        JsonNode first = MAPPER.readTree(rows[0]);
        assertThat(first.get("name").asText()).isEqualTo("EMAIL");
        assertThat(first.get("count").asLong()).isEqualTo(2L);
        // 并列 1 次按名字典序：CN_PHONE < PROJECT...
        JsonNode second = MAPPER.readTree(rows[1]);
        assertThat(second.get("name").asText()).isEqualTo("CN_PHONE");
        JsonNode third = MAPPER.readTree(rows[2]);
        assertThat(third.get("name").asText()).startsWith("PROJECT");
        assertThat(third.get("count").asLong()).isEqualTo(1L);
    }

    @Test
    void emptyStatsExportsZeroLinesHonest() throws Exception {
        StringWriter out = new StringWriter();
        assertThat(PiiHitStatsJsonl.export(PiiHitStats.create(), out)).isZero();
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void exportThenResetYieldsNextWindowFromScratch() throws Exception {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.BANK_CARD);
        StringWriter windowOne = new StringWriter();
        assertThat(PiiHitStatsJsonl.export(stats, windowOne)).isEqualTo(1);

        stats.reset(); // 窗口切换：报表归零
        StringWriter windowTwo = new StringWriter();
        assertThat(PiiHitStatsJsonl.export(stats, windowTwo)).isZero();
        assertThat(windowTwo.toString()).isEmpty();

        stats.record(PiiType.IPV4);
        StringWriter windowThree = new StringWriter();
        PiiHitStatsJsonl.export(stats, windowThree);
        assertThat(MAPPER.readTree(windowThree.toString().split("\n")[0]).get("name").asText())
                .isEqualTo("IPV4");
    }
}
