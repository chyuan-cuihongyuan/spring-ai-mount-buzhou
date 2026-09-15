package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1857 / T2916：保工作性——违例判定、浪费覆盖比、健康面。 */
class WorkConservationAuditTest {

    /** 违例判定：一队列积压、另一队列容量闲置即违例。 */
    @Test
    void shouldDetectIdleCapacityWithBacklog() {
        WorkConservationAudit.Report report = WorkConservationAudit.audit(List.of(
                List.of(new WorkConservationAudit.Slot("a", 100, 0),
                        new WorkConservationAudit.Slot("b", 0, 5)),
                List.of(new WorkConservationAudit.Slot("a", 50, 1),
                        new WorkConservationAudit.Slot("b", 50, 1))));
        assertThat(report.slots()).isEqualTo(2);
        assertThat(report.violations()).isEqualTo(1);
        assertThat(report.totalIdleWithBacklog()).isEqualTo(5);
        assertThat(report.totalBacklogDuringViolation()).isEqualTo(100);
        assertThat(report.wasteCoverageRatio()).isEqualTo(0.05d);
        assertThat(report.violationRatio()).isEqualTo(0.5d);
    }

    /** 健康面：容量随活走（全忙或全空转无积压）零违例。 */
    @Test
    void healthySchedulesReadZeroViolations() {
        WorkConservationAudit.Report allBusy = WorkConservationAudit.audit(List.of(
                List.of(new WorkConservationAudit.Slot("a", 10, 2),
                        new WorkConservationAudit.Slot("b", 10, 2))));
        assertThat(allBusy.violations()).isZero();
        assertThat(allBusy.wasteCoverageRatio()).isEqualTo(-1d);

        WorkConservationAudit.Report idleNoBacklog = WorkConservationAudit.audit(
                List.of(List.of(new WorkConservationAudit.Slot("a", 0, 3))));
        assertThat(idleNoBacklog.violations()).isZero();
    }

    /** 空输入与 null：零时隙哨兵。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (WorkConservationAudit.Report r : List.of(
                WorkConservationAudit.audit(List.of()),
                WorkConservationAudit.audit(null))) {
            assertThat(r.slots()).isZero();
            assertThat(r.violationRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：空白队列名、负积压/容量。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new WorkConservationAudit.Slot("", 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法时隙");
        assertThatThrownBy(() -> new WorkConservationAudit.Slot("a", -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkConservationAudit.Slot("a", 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
