package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 724 / T1048–T1049：状态 TTL 覆盖审计——coverage 精确、producer 归因、
 * 空真、null fail-fast。
 */
class StateTtlCoverageTest {

    private static StateEntry entry(String producer, Integer ttlTurns) {
        return new StateEntry("k", "v", producer, 0, ttlTurns,
                Instant.parse("2026-09-13T00:00:00Z"));
    }

    @Test
    void coverageAndProducerAttributionAreExact() {
        Map<String, StateEntry> entries = new LinkedHashMap<>();
        entries.put("k1", entry("hook-a", 10));
        entries.put("k2", entry("hook-a", null));
        entries.put("k3", entry("advisor-b", null));
        entries.put("k4", entry("advisor-b", 3));
        entries.put("k5", entry("eval", 1));
        StateTtlCoverage.Report report = StateTtlCoverage.analyze(entries);
        assertThat(report.totalKeys()).isEqualTo(5);
        assertThat(report.persistentKeys()).isEqualTo(2);
        assertThat(report.ttlKeys()).isEqualTo(3);
        assertThat(report.coverage()).isEqualTo(3.0 / 5);
        assertThat(report.byProducer()).extracting(StateTtlCoverage.Row::producer)
                .containsExactly("advisor-b", "eval", "hook-a"); // 字典序
        assertThat(report.byProducer().get(0).persistent()).isEqualTo(1); // advisor-b: k3 永生
    }

    @Test
    void emptyMapIsVacuousTruthAndNullFailsFast() {
        StateTtlCoverage.Report empty = StateTtlCoverage.analyze(Map.of());
        assertThat(empty.totalKeys()).isZero();
        assertThat(empty.coverage()).isEqualTo(1.0); // 空真——无键可泄漏
        assertThat(empty.byProducer()).isEmpty();
        assertThatThrownBy(() -> StateTtlCoverage.analyze(null))
                .isInstanceOf(NullPointerException.class);
    }
}
