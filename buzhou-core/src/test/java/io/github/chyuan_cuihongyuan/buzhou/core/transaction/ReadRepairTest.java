package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.transaction.ReadRepair.Report;
import io.github.chyuan_cuihongyuan.buzhou.core.transaction.ReadRepair.Stitch;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5018 / T6138：Read Repair 合同——择优、陈旧清单、
 * tie-break、畸形 fail-fast、确定性。
 */
class ReadRepairTest {

    @Test
    void consistentReportsShouldHaveEmptyStale() {
        Stitch stitch = ReadRepair.stitch(List.of(
                new Report("r1", 5), new Report("r2", 5), new Report("r3", 5)));
        assertThat(stitch.winner().replica()).isEqualTo("r1");   // 全并列——字典序 tie-break
        assertThat(stitch.staleReplicas()).isEmpty();
    }

    @Test
    void divergentVersionsShouldPickMaxAndListStale() {
        Stitch stitch = ReadRepair.stitch(List.of(
                new Report("r1", 3), new Report("r2", 9), new Report("r3", 6)));
        assertThat(stitch.winner().replica()).isEqualTo("r2");   // 版本 9 最高胜
        assertThat(stitch.winner().version()).isEqualTo(9L);
        assertThat(stitch.staleReplicas()).containsExactly("r1", "r3");   // 字典序
    }

    @Test
    void versionTieShouldBreakByReplicaName() {
        Stitch stitch = ReadRepair.stitch(List.of(
                new Report("beta", 7), new Report("alpha", 7)));
        assertThat(stitch.winner().replica()).isEqualTo("alpha");   // 并列字典序最小
        assertThat(stitch.staleReplicas()).isEmpty();
    }

    @Test
    void sameInputShouldReplaySameStitch() {
        List<Report> reports = List.of(new Report("r1", 3), new Report("r2", 9), new Report("r3", 6));
        Stitch first = ReadRepair.stitch(reports);
        Stitch second = ReadRepair.stitch(reports);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void emptyAndInvalidReportsShouldFailFast() {
        assertThatThrownBy(() -> ReadRepair.stitch(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReadRepair.stitch(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReadRepair.stitch(List.of(new Report("", 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReadRepair.stitch(List.of(new Report("r1", -1))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
