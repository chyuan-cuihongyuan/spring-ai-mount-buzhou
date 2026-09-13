package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-693 / spec 944：工具调用结局分布——四桶精确、total 守恒（含 other
 * 收容桶）、空日志全 0、null fail-fast。
 */
class OutcomeStatsTest {

    private static ToolCallLogEntry entry(ToolCallOutcome outcome) {
        return new ToolCallLogEntry("s1", "tc-1", "tool", "hash",
                outcome, "", Instant.EPOCH);
    }

    @Test
    void fourBucketsCountedExactly() {
        var stats = ToolCallOutcomeStats.stats(List.of(
                entry(ToolCallOutcome.COMPLETED),
                entry(ToolCallOutcome.COMPLETED),
                entry(ToolCallOutcome.FAILED),
                entry(ToolCallOutcome.TIMEOUT),
                entry(ToolCallOutcome.CANCELLED),
                entry(ToolCallOutcome.VALIDATION_REJECTED)));

        assertThat(stats.completed()).isEqualTo(2);
        assertThat(stats.failed()).isEqualTo(1);
        assertThat(stats.timeouts()).isEqualTo(1);
        assertThat(stats.cancelled()).isEqualTo(1);
        assertThat(stats.other()).isEqualTo(1); // VALIDATION_REJECTED 落 other
        assertThat(stats.total()).isEqualTo(6);
    }

    @Test
    void emptyLogAllZero() {
        var stats = ToolCallOutcomeStats.stats(List.of());
        assertThat(stats.total()).isZero();
    }

    @Test
    void nullEntriesFailsFast() {
        assertThatThrownBy(() -> ToolCallOutcomeStats.stats(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
