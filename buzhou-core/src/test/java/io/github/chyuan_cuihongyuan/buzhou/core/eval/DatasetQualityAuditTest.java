package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1437 / T2176：评估数据集质量审计——空 input/expected 分桶、短输入
 * 阈值、长度 P50/P95、退化比派生、空集哨兵；与近重复/期望格式轴辨义。
 */
class DatasetQualityAuditTest {

    private static EvalItem item(String id, String input, String expected) {
        return new EvalItem(id, input, expected, null, null,
                Instant.parse("2026-09-14T10:00:00Z"));
    }

    @Test
    void emptyDatasetYieldsZeroWithSentinel() {
        var r = DatasetQualityAudit.analyze(List.of());
        assertThat(r.totalItems()).isZero();
        assertThat(r.degenerateRatio()).isEqualTo(-1d);
        assertThat(r.inputLengthP50()).isZero();
    }

    @Test
    void degenerateEntriesCounted() {
        var r = DatasetQualityAudit.analyze(List.of(
                item("d1", "如何退货退款流程是什么", "退货流程说明"),
                item("d2", "", "答案"),
                item("d3", "   ", "答案"),
                item("d4", "合法问题", null)));
        assertThat(r.totalItems()).isEqualTo(4);
        assertThat(r.emptyInputs()).isEqualTo(2);
        assertThat(r.emptyExpecteds()).isEqualTo(1);
        // 退化比 = (2+1)/4 = 0.75
        assertThat(r.degenerateRatio()).isEqualTo(0.75d);
    }

    @Test
    void shortInputsBelowThresholdCounted() {
        var r = DatasetQualityAudit.analyze(List.of(
                item("d1", "如何退货退款", "r1"),        // 6 字符 < 8
                item("d2", "如何申请退货退款流程", "r2"))); // 10 字符 ≥ 8
        assertThat(r.shortInputs()).isEqualTo(1);
        assertThat(r.emptyInputs()).isZero();
    }

    @Test
    void lengthPercentilesComputed() {
        var r = DatasetQualityAudit.analyze(List.of(
                item("d1", "1234567890", "r"),   // 10
                item("d2", "12345678901234567890", "r"), // 20
                item("d3", "123456789012345", "r")));    // 15
        // 排序 10/15/20：P50=15，P95=20
        assertThat(r.inputLengthP50()).isEqualTo(15);
        assertThat(r.inputLengthP95()).isEqualTo(20);
    }

    @Test
    void blankExpectedAndEmptyInputBothCountTowardDegenerate() {
        var r = DatasetQualityAudit.analyze(List.of(
                item("d1", null, null)));
        assertThat(r.emptyInputs()).isEqualTo(1);
        assertThat(r.emptyExpecteds()).isEqualTo(1);
        assertThat(r.degenerateRatio()).isEqualTo(2.0d); // 双桶同计（同条目两轴都退化）
    }
}
