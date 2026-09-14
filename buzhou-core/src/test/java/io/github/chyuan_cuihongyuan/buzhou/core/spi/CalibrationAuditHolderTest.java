package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校准审计 Holder 接线测试（spec 1618 / T2387–T2388 / impl 1171）：
 * Holder 读出面 + 审计器对账语义（spec 819 孤类接线）。
 */
class CalibrationAuditHolderTest {

    @AfterEach
    void reset() {
        CalibrationAuditHolder.install(null);
    }

    @Test
    void holderExposesAuditAndCalibration() {
        CalibrationAuditHolder.audit().record(120, 100); // 高估 20%
        CalibrationAuditHolder.audit().record(80, 100);  // 低估 20%
        CalibrationAuditHolder.audit().record(90, 0);    // actual=0 忽略

        EstimatorCalibrationAudit.Calibration calibration = CalibrationAuditHolder.calibration();
        assertThat(calibration.empty()).isFalse();
        assertThat(calibration.pairs()).isEqualTo(2);
        // 均值相对误差 = (+0.2 + -0.2)/2 = 0；偏高/偏低各半
        assertThat(calibration.meanRelativeError()).isZero();
        assertThat(calibration.biasOverRatio()).isEqualTo(0.5);
    }

    @Test
    void installReplacesInstance() {
        EstimatorCalibrationAudit fresh = new EstimatorCalibrationAudit();
        CalibrationAuditHolder.install(fresh);
        fresh.record(50, 100);
        assertThat(CalibrationAuditHolder.calibration().pairs()).isEqualTo(1);
        CalibrationAuditHolder.install(null); // 重置
        assertThat(CalibrationAuditHolder.calibration().pairs()).isZero();
    }
}
