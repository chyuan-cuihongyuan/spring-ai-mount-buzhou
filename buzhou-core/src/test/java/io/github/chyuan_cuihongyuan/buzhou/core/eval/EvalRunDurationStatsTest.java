package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 544 / T837：run 项耗时分布——exact 最近秩 p50/p95、最慢项 top3
 * （降序+同值字典序稳定）、零样本 null、null run fail-fast。
 */
class EvalRunDurationStatsTest {

    private static EvalRunItemResult item(String id, String status, long ms) {
        return new EvalRunItemResult(id, status, "", "", ms);
    }

    private static EvalRunResult run(List<EvalRunItemResult> items) {
        return new EvalRunResult("r", "ds", Instant.EPOCH, Instant.EPOCH,
                items.size(), 0, 0, 0, items);
    }

    @Test
    void percentilesAndSlowestTop3() {
        var run = run(List.of(
                item("a", "pass", 100), item("b", "pass", 200),
                item("c", "error", 5000), item("d", "pass", 400),
                item("e", "fail", 300), item("f", "pass", 250)));
        var stats = EvalRunDurationStats.analyze(run);
        assertThat(stats.items()).isEqualTo(6);
        assertThat(stats.p50Millis()).isEqualTo(250);
        assertThat(stats.p95Millis()).isEqualTo(5000);
        assertThat(stats.maxMillis()).isEqualTo(5000);
        // 最慢 top3 降序；同值字典序稳定
        assertThat(stats.slowest().get(0).itemId()).isEqualTo("c");
        assertThat(stats.slowest().get(0).durationMs()).isEqualTo(5000);
    }

    @Test
    void emptyRunGivesNullPercentiles() {
        var stats = EvalRunDurationStats.analyze(run(List.of()));
        assertThat(stats.items()).isZero();
        assertThat(stats.p50Millis()).isNull();
        assertThat(stats.p95Millis()).isNull();
        assertThat(stats.maxMillis()).isZero();
    }

    @Test
    void nullRunFailsFast() {
        assertThatThrownBy(() -> EvalRunDurationStats.analyze(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
