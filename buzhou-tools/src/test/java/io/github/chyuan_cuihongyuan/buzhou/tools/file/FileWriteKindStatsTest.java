package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1730 / T2662：FileWriteKindStats 直测——三型归账/变更占比/reset。
 */
class FileWriteKindStatsTest {

    @Test
    void emptyCarriesSentinel() {
        var stats = new FileWriteKindStats();
        assertThat(stats.census().changedShare()).isEqualTo(-1d);
    }

    @Test
    void kindsTallyWithChangedShare() {
        var stats = new FileWriteKindStats();
        stats.record(FileWriteKindStats.WriteKind.CREATE);
        stats.record(FileWriteKindStats.WriteKind.OVERWRITE_CHANGED);
        stats.record(FileWriteKindStats.WriteKind.OVERWRITE_UNCHANGED);
        stats.record(FileWriteKindStats.WriteKind.OVERWRITE_UNCHANGED);
        var census = stats.census();
        assertThat(census.total()).isEqualTo(4);
        assertThat(census.creates()).isEqualTo(1);
        assertThat(census.overwriteUnchanged()).isEqualTo(2);
        assertThat(census.overwriteChanged()).isEqualTo(1);
        assertThat(census.changedShare()).isCloseTo(0.5d, within(1e-9));
    }

    @Test
    void resetForTest() {
        var stats = new FileWriteKindStats();
        stats.record(FileWriteKindStats.WriteKind.CREATE);
        stats.resetForTest();
        assertThat(stats.census().total()).isZero();
    }
}
