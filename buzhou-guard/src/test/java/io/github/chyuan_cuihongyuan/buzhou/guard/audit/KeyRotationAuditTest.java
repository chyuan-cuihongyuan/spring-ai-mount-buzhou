package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 807 / T1116：密钥轮换到期审计回归——三档边界精确/最坏排序/
 * UNKNOWN_ACTIVE/无钥降级/脏账跳过/fail-fast。
 */
class KeyRotationAuditTest {

    private static final long NOW = 1_000_000L;

    @Test
    void severityBoundariesExact() {
        // maxAge=1000, warnBefore=200：DUE_SOON 起点=age≥800
        Map<Integer, Long> times = Map.of(
                1, NOW - 1000, // OVERDUE（恰达 maxAge）
                2, NOW - 999,  // DUE_SOON（临期窗 800..999）
                3, NOW - 800,  // DUE_SOON（恰达临期线）
                4, NOW - 500); // OK（距临期线尚远）
        var report = KeyRotationAudit.audit(times, 1, true, NOW, 1000, 200);

        var byVersion = report.findings().stream().collect(
                java.util.stream.Collectors.toMap(KeyRotationAudit.Finding::version, f -> f));
        assertThat(byVersion.get(1).severity()).isEqualTo("OVERDUE");
        assertThat(byVersion.get(2).severity()).isEqualTo("DUE_SOON");
        assertThat(byVersion.get(3).severity()).isEqualTo("DUE_SOON");
        assertThat(byVersion.get(4).severity()).isEqualTo("OK");
    }

    @Test
    void worstFirstSortingWithAgeDescTies() {
        Map<Integer, Long> times = Map.of(
                5, NOW - 100,  // OK
                6, NOW - 2000, // OVERDUE 最老
                7, NOW - 1500, // OVERDUE 次老
                8, NOW - 900); // DUE_SOON
        var report = KeyRotationAudit.audit(times, 6, true, NOW, 1000, 200);

        assertThat(report.findings().stream().map(KeyRotationAudit.Finding::version))
                .containsExactly(6, 7, 8, 5); // OVERDUE 老→新、DUE_SOON、OK
        assertThat(report.findings().get(0).ageMillis()).isEqualTo(2000);
    }

    @Test
    void unknownActiveVersionSurfaces() {
        var report = KeyRotationAudit.audit(
                Map.of(3, NOW - 100), 7, true, NOW, 10_000, 1_000);
        assertThat(report.findings()).hasSize(2);
        assertThat(report.findings().get(0).severity()).isEqualTo("UNKNOWN_ACTIVE");
        assertThat(report.findings().get(0).version()).isEqualTo(7);
        assertThat(report.findings().get(1).severity()).isEqualTo("OK");
    }

    @Test
    void keylessDegradationReportedFaithfully() {
        var report = KeyRotationAudit.audit(Map.of(), null, false, NOW, 10_000, 1_000);
        assertThat(report.findings()).isEmpty();
        assertThat(report.hasSigningKey()).isFalse();
        assertThat(report.activeVersion()).isNull();
    }

    @Test
    void dirtyLedgerEntriesSkipped() {
        var report = KeyRotationAudit.audit(
                java.util.Map.of(1, NOW - 100, 2, -5L), 1, true, NOW, 10_000, 100);
        assertThat(report.findings()).hasSize(1);
        assertThat(report.findings().get(0).version()).isEqualTo(1);
    }

    @Test
    void failFastOnBadPolicy() {
        assertThatThrownBy(() -> KeyRotationAudit.audit(Map.of(), null, false, NOW, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KeyRotationAudit.audit(Map.of(), null, false, NOW, 100, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KeyRotationAudit.audit(Map.of(), null, false, NOW, 100, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KeyRotationAudit.audit(null, null, false, NOW, 100, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
