package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 313 / impl-336：PII 命中分侧回归——分侧计数/排行稳定序/单参兼容
 * UNSPECIFIED/JSONL 三列/overflow 与分侧共存。
 */
class PiiHitStatsSideTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sideCountingSplitsInputAndOutput() {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.EMAIL, PiiHitStats.Side.OUTPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.OUTPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.INPUT);

        assertThat(stats.countOf("EMAIL", PiiHitStats.Side.OUTPUT)).isEqualTo(2);
        assertThat(stats.countOf("EMAIL", PiiHitStats.Side.INPUT)).isEqualTo(1);
        assertThat(stats.countOf("EMAIL", PiiHitStats.Side.UNSPECIFIED)).isZero();
        assertThat(stats.countOf("EMAIL")).isEqualTo(3); // 总量口径不变
    }

    @Test
    void topBySideStableOrder() {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.CN_PHONE, PiiHitStats.Side.INPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.INPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.INPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.OUTPUT); // 输出侧不入输入榜

        assertThat(stats.topBySide(PiiHitStats.Side.INPUT, 10))
                .extracting(PiiHitStats.Hit::name, PiiHitStats.Hit::count)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("EMAIL", 2L),
                        org.assertj.core.groups.Tuple.tuple("CN_PHONE", 1L));
        assertThat(stats.topBySide(PiiHitStats.Side.OUTPUT, 10))
                .extracting(PiiHitStats.Hit::name)
                .containsExactly("EMAIL");
    }

    @Test
    void singleArgRecordDefaultsToUnspecified() {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.EMAIL); // 旧调用方
        stats.recordCustom("LEGACY_RULE");

        assertThat(stats.countOf("EMAIL", PiiHitStats.Side.UNSPECIFIED)).isEqualTo(1);
        assertThat(stats.countOf("LEGACY_RULE", PiiHitStats.Side.UNSPECIFIED)).isEqualTo(1);
    }

    @Test
    void jsonlCarriesSideColumns() throws Exception {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.EMAIL, PiiHitStats.Side.OUTPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.OUTPUT);
        stats.record(PiiType.EMAIL, PiiHitStats.Side.INPUT);
        StringWriter out = new StringWriter();

        long lines = PiiHitStatsJsonl.export(stats, out);

        assertThat(lines).isEqualTo(1);
        JsonNode line = MAPPER.readTree(out.toString());
        assertThat(line.get("count").asLong()).isEqualTo(3);
        assertThat(line.get("inputCount").asLong()).isEqualTo(1);
        assertThat(line.get("outputCount").asLong()).isEqualTo(2);
    }

    @Test
    void overflowFoldsAcrossSides() {
        PiiHitStats stats = PiiHitStats.create();
        for (int i = 0; i < PiiHitStats.MAX_CUSTOM_RULES + 5; i++) {
            stats.recordCustom("RULE_" + i, PiiHitStats.Side.INPUT);
        }
        assertThat(stats.countOf(PiiHitStats.OVERFLOW, PiiHitStats.Side.INPUT))
                .as("封顶后新名折 overflow（侧随之）").isEqualTo(5);
        assertThat(stats.distinct()).isEqualTo(PiiHitStats.MAX_CUSTOM_RULES + 1);
    }
}
