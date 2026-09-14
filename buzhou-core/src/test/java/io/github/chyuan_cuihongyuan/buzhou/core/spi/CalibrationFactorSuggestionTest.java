package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校准系数建议测试（spec 1636 / T2423–T2424 / impl 1189）：
 * 持续高估给出 <1 修正系数、低估 >1、样本不足/零偏差 empty。
 */
class CalibrationFactorSuggestionTest {

    @AfterEach
    void reset() {
        CalibrationAuditHolder.install(null);
    }

    @Test
    void persistentOverestimateSuggestsDownwardFactor() {
        for (int i = 0; i < 20; i++) {
            CalibrationAuditHolder.audit().record(125, 100); // 恒高估 25%
        }
        double suggestion = CalibrationAuditHolder.calibrationFactorSuggestion(10).orElseThrow();
        assertThat(suggestion).isLessThan(1.0).isGreaterThan(0.7); // ≈0.8（1/1.25）
    }

    @Test
    void underestimatesSuggestUpwardFactor() {
        for (int i = 0; i < 20; i++) {
            CalibrationAuditHolder.audit().record(80, 100); // 恒低估
        }
        double suggestion = CalibrationAuditHolder.calibrationFactorSuggestion(10).orElseThrow();
        assertThat(suggestion).isGreaterThan(1.0);
    }

    @Test
    void insufficientSamplesOrZeroBiasEmpty() {
        for (int i = 0; i < 5; i++) {
            CalibrationAuditHolder.audit().record(120, 100);
        }
        assertThat(CalibrationAuditHolder.calibrationFactorSuggestion(10)).isEmpty(); // 样本不足
        CalibrationAuditHolder.install(null);
        for (int i = 0; i < 20; i++) {
            CalibrationAuditHolder.audit().record(100, 100); // 零偏差
        }
        assertThat(CalibrationAuditHolder.calibrationFactorSuggestion(10)).isEmpty();
    }
}
