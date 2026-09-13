package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 819 / T1140：估算校准审计回归——相对误差口径/偏高偏低占比/近窗 P95/
 * 脏样本忽略/空真。
 */
class EstimatorCalibrationAuditTest {

    @Test
    void relativeErrorAndBiasCounts() {
        EstimatorCalibrationAudit audit = new EstimatorCalibrationAudit();
        audit.record(120, 100); // 高估 +0.2
        audit.record(90, 100);  // 低估 -0.1
        audit.record(100, 100); // 精确 0.0

        EstimatorCalibrationAudit.Calibration cal = audit.audit();
        assertThat(cal.pairs()).isEqualTo(3);
        assertThat(cal.meanRelativeError()).isCloseTo((0.2 - 0.1 + 0.0) / 3, within(1e-9));
        assertThat(cal.biasOverRatio()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(audit.biasUnderRatio()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(cal.empty()).isFalse();
    }

    @Test
    void recentP95AbsError() {
        EstimatorCalibrationAudit audit = new EstimatorCalibrationAudit();
        for (int i = 1; i <= 100; i++) {
            audit.record(i, 100); // 误差 |i-100|：1..100 → abs 99..0..?
        }
        // 样本 i=1..100：absError = |i-100|，最大 99（i=1）
        // 近窗全部 100 对；P95 = 升序第 95 位
        EstimatorCalibrationAudit.Calibration cal = audit.audit();
        assertThat(cal.pairs()).isEqualTo(100);
        // abs 序列 {0..99} 各一次；升序第 ⌈0.95·100⌉=95 位（1-based）= 94
        assertThat(cal.recentP95AbsError()).isEqualTo(94.0);
    }

    @Test
    void dirtyPairsIgnored() {
        EstimatorCalibrationAudit audit = new EstimatorCalibrationAudit();
        audit.record(-5, 100);
        audit.record(50, 0);
        audit.record(50, -1);
        assertThat(audit.audit().pairs()).isZero();
        assertThat(audit.audit().empty()).isTrue();
    }

    @Test
    void emptyAuditAllZero() {
        EstimatorCalibrationAudit.Calibration cal = new EstimatorCalibrationAudit().audit();
        assertThat(cal.pairs()).isZero();
        assertThat(cal.meanRelativeError()).isZero();
        assertThat(cal.biasOverRatio()).isZero();
        assertThat(cal.recentP95AbsError()).isZero();
        assertThat(cal.empty()).isTrue();
        assertThat(new EstimatorCalibrationAudit().biasUnderRatio()).isZero();
    }

    @Test
    void windowEvictsOldest() {
        EstimatorCalibrationAudit audit = new EstimatorCalibrationAudit();
        for (int i = 0; i < EstimatorCalibrationAudit.WINDOW + 20; i++) {
            audit.record(200, 100); // 误差 100
        }
        // 仍只保留近窗 128 对的口径——pairs 累计 148
        assertThat(audit.audit().pairs()).isEqualTo(EstimatorCalibrationAudit.WINDOW + 20);
        assertThat(audit.audit().recentP95AbsError()).isEqualTo(100.0);
    }
}
