package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 717 / T1034–T1035：共享事实冲突审计——CONFLICT/DUPLICATE 判定、
 * entries 证据全列、健康集零发现、空表与 null。
 */
class FactConflictAuditTest {

    private static SharedFact fact(String key, Object value, String owner) {
        return new SharedFact(key, value, owner, Instant.parse("2026-09-12T00:00:00Z"),
                Duration.ofHours(1));
    }

    @Test
    void conflictingValuesAreReportedWithFullEvidence() {
        FactConflictAudit.Report report = FactConflictAudit.audit(List.of(
                fact("user.language", "zh", "agent-a1"),
                fact("user.language", "en", "agent-a2"),
                fact("user.name", "小明", "agent-a1")));
        assertThat(report.conflictKeys()).isEqualTo(1);
        assertThat(report.duplicateKeys()).isZero();
        assertThat(report.keysScanned()).isEqualTo(2);
        FactConflictAudit.Row conflict = report.rows().get(0);
        assertThat(conflict.kind()).isEqualTo(FactConflictAudit.Kind.CONFLICT);
        assertThat(conflict.key()).isEqualTo("user.language");
        assertThat(conflict.entries()).containsExactlyInAnyOrder(
                "agent-a1=zh", "agent-a2=en"); // 裁决证据全列
    }

    @Test
    void duplicatePublicationIsReportedSeparately() {
        FactConflictAudit.Report report = FactConflictAudit.audit(List.of(
                fact("team.name", "platform", "agent-a1"),
                fact("team.name", "platform", "agent-a2")));
        assertThat(report.rows()).hasSize(1);
        assertThat(report.rows().get(0).kind()).isEqualTo(FactConflictAudit.Kind.DUPLICATE);
        assertThat(report.duplicateKeys()).isEqualTo(1);
        assertThat(report.conflictKeys()).isZero();
    }

    @Test
    void healthySnapshotHasNoFindings() {
        FactConflictAudit.Report report = FactConflictAudit.audit(List.of(
                fact("a", 1, "o1"),
                fact("b", "x", "o2"),
                fact("c", true, "o3")));
        assertThat(report.rows()).isEmpty();
        assertThat(report.conflictKeys()).isZero();
        assertThat(report.keysScanned()).isEqualTo(3);
    }

    @Test
    void emptyAndNullAndDeterministicOrder() {
        assertThat(FactConflictAudit.audit(List.of()).rows()).isEmpty();
        assertThatThrownBy(() -> FactConflictAudit.audit(null))
                .isInstanceOf(NullPointerException.class);
        // 字典序：冲突键 b 排在 d 前
        FactConflictAudit.Report report = FactConflictAudit.audit(List.of(
                fact("d", 1, "o1"),
                fact("d", 2, "o2"),
                fact("b", 1, "o1"),
                fact("b", 9, "o2")));
        assertThat(report.rows()).extracting(FactConflictAudit.Row::key)
                .containsExactly("b", "d");
    }
}
