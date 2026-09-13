package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolSlowLogTest {

    @BeforeEach
    @AfterEach
    void resetReadout() {
        ToolSlowLog.reset();
    }

    @Test
    void belowDefaultThresholdIsNotRecorded() {
        ToolSlowLog.record("fast_tool", 1_000_000, false);

        assertThat(ToolSlowLog.entries()).isEmpty();
    }

    @Test
    void strictlyAboveThresholdIsRecordedWithFields() {
        ToolSlowLog.configureThreshold(Duration.ofMillis(10));

        ToolSlowLog.record("slow_tool", 20_000_000, true);

        List<ToolSlowLog.Entry> entries = ToolSlowLog.entries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).toolName()).isEqualTo("slow_tool");
        assertThat(entries.get(0).durationMillis()).isEqualTo(20);
        assertThat(entries.get(0).failed()).isTrue();
        assertThat(entries.get(0).epochMillis()).isPositive();
    }

    @Test
    void exactlyAtThresholdIsNotRecorded() {
        ToolSlowLog.configureThreshold(Duration.ofMillis(10));

        ToolSlowLog.record("edge_tool", 10_000_000, false);

        assertThat(ToolSlowLog.entries()).isEmpty();
    }

    @Test
    void ringIsBoundedFifoNewestFirst() {
        ToolSlowLog.configureThreshold(Duration.ZERO);

        int total = ToolSlowLog.CAPACITY + 8;
        for (int i = 0; i < total; i++) {
            ToolSlowLog.record("t_" + i, 1, false);
        }

        List<ToolSlowLog.Entry> entries = ToolSlowLog.entries();
        assertThat(entries).hasSize(ToolSlowLog.CAPACITY);
        assertThat(entries.get(0).toolName()).isEqualTo("t_" + (total - 1));
        assertThat(entries.get(entries.size() - 1).toolName()).isEqualTo("t_8");
    }

    @Test
    void entriesSnapshotIsImmutable() {
        ToolSlowLog.configureThreshold(Duration.ZERO);
        ToolSlowLog.record("a", 1, false);

        List<ToolSlowLog.Entry> snapshot = ToolSlowLog.entries();

        assertThatThrownBy(() -> snapshot.add(
                new ToolSlowLog.Entry("b", 1, 1, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void zeroThresholdRecordsAnyPositiveDuration() {
        ToolSlowLog.configureThreshold(Duration.ZERO);

        ToolSlowLog.record("zero_dur", 0, false);
        assertThat(ToolSlowLog.entries()).isEmpty();

        ToolSlowLog.record("one_nano", 1, false);
        assertThat(ToolSlowLog.entries()).hasSize(1);
        assertThat(ToolSlowLog.entries().get(0).toolName()).isEqualTo("one_nano");
    }

    @Test
    void resetClearsEntriesAndRestoresDefaultThreshold() {
        ToolSlowLog.configureThreshold(Duration.ZERO);
        ToolSlowLog.record("a", 1, false);
        assertThat(ToolSlowLog.entries()).hasSize(1);

        ToolSlowLog.reset();

        assertThat(ToolSlowLog.entries()).isEmpty();
        assertThat(ToolSlowLog.thresholdMillis()).isEqualTo(ToolSlowLog.DEFAULT_SLOWER_THAN_MILLIS);
        ToolSlowLog.record("tiny", 1, false);
        assertThat(ToolSlowLog.entries()).isEmpty();
    }

    @Test
    void negativeThresholdIsRejected() {
        assertThatThrownBy(() -> ToolSlowLog.configureThreshold(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ToolSlowLog.configureThreshold(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
