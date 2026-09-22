package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1879 / T2960：协同遗漏——阶梯补账、盲窗、覆盖率、畸形 fail-fast。 */
class CoordinatedOmissionAuditTest {

    /** 阶梯：450/100 补 4 样本；不超间隔/恰好间隔/零时延均 1。 */
    @Test
    void ladderCounts() {
        assertThat(CoordinatedOmissionAudit.correctedSampleCount(450, 100)).isEqualTo(4L);
        assertThat(CoordinatedOmissionAudit.correctedSampleCount(100, 100)).isEqualTo(1L);
        assertThat(CoordinatedOmissionAudit.correctedSampleCount(50, 100)).isEqualTo(1L);
        assertThat(CoordinatedOmissionAudit.correctedSampleCount(0, 100)).isEqualTo(1L);
    }

    /** 遗漏与盲窗：450 慢响应掩盖 3 次发送、静默 350ms；快响应零盲区。 */
    @Test
    void omittedAndBlindWindow() {
        assertThat(CoordinatedOmissionAudit.omittedCount(450, 100)).isEqualTo(3L);
        assertThat(CoordinatedOmissionAudit.omittedCount(100, 100)).isZero();
        assertThat(CoordinatedOmissionAudit.blindWindow(450, 100)).isEqualTo(350L);
        assertThat(CoordinatedOmissionAudit.blindWindow(50, 100)).isZero();
    }

    /** 覆盖率：观测 10 校正 40 = 0.25——原始记录只见四分之一需求。 */
    @Test
    void coverageRatioScales() {
        assertThat(CoordinatedOmissionAudit.coverageRatio(10, 40)).isCloseTo(0.25, within(1e-9));
        assertThat(CoordinatedOmissionAudit.coverageRatio(40, 40)).isEqualTo(1.0);
    }

    /** 畸形入参 fail-fast：负时延、零间隔、校正 0、校正 < 观测。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> CoordinatedOmissionAudit.correctedSampleCount(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时延不能为负");
        assertThatThrownBy(() -> CoordinatedOmissionAudit.correctedSampleCount(100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedIntervalMillis 不能小于 1");
        assertThatThrownBy(() -> CoordinatedOmissionAudit.coverageRatio(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correctedSamples 不能小于 1");
        assertThatThrownBy(() -> CoordinatedOmissionAudit.coverageRatio(50, 40))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("观测样本须在 [0, 校正=40]");
    }
}
