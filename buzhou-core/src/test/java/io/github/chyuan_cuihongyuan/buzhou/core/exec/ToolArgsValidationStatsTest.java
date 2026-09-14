package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1410 / T2122：工具入参校验读数——总量守恒（validations=accepted+rejected）、
 * 七错误桶按标记单源分桶（桶非互斥）、无 schema 不入账、reset 归零；静态面
 * 进程级先例（BuzhouMetricsHolder），测试前后归零防串扰。
 */
class ToolArgsValidationStatsTest {

    private static final String SCHEMA = """
            {"type":"object","properties":{
              "city":{"type":"string"},
              "days":{"type":"integer","minimum":1,"maximum":30}},
              "required":["city"]}
            """;

    @BeforeEach
    void resetBefore() {
        ToolArgsValidator.resetValidationStatsForTest();
    }

    @AfterEach
    void resetAfter() {
        ToolArgsValidator.resetValidationStatsForTest();
    }

    @Test
    void conservationAcrossAcceptAndReject() {
        ToolArgsValidator.validate(SCHEMA, "{\"city\":\"北京\",\"days\":3}");
        ToolArgsValidator.validate(SCHEMA, "{\"city\":\"上海\"}"); // days 可选 → 通过
        ToolArgsValidator.validate(SCHEMA, "{}"); // 缺 city → 拒绝
        var s = ToolArgsValidator.validationStats();
        assertThat(s.validations()).isEqualTo(3);
        assertThat(s.accepted()).isEqualTo(2);
        assertThat(s.rejectedDerived()).isEqualTo(1);
        assertThat(s.validations()).isEqualTo(s.accepted() + s.rejected());
        assertThat(s.missingRequired()).isEqualTo(1);
    }

    @Test
    void unparseableArgsBucketed() {
        ToolArgsValidator.validate(SCHEMA, "{not json");
        var s = ToolArgsValidator.validationStats();
        assertThat(s.validations()).isEqualTo(1);
        assertThat(s.rejected()).isEqualTo(1);
        assertThat(s.unparseableArgs()).isEqualTo(1);
    }

    @Test
    void errorBucketsAreNonExclusiveWithinOneValidation() {
        // 一条入参同时缺必填（city 缺失）+ 类型错（days 非整数）：两桶各 +1，rejected 只 +1
        ToolArgsValidator.validate(SCHEMA, "{\"days\":\"abc\"}");
        var s = ToolArgsValidator.validationStats();
        assertThat(s.rejected()).isEqualTo(1);
        assertThat(s.typeMismatch()).isEqualTo(1);
        assertThat(s.missingRequired()).isEqualTo(1);
        assertThat(s.validations()).isEqualTo(s.accepted() + s.rejected());
    }

    @Test
    void rangeAndEnumViolationsBucketed() {
        String enumSchema = """
                {"type":"object","properties":{"unit":{"type":"string","enum":["c","f"]}}}
                """;
        ToolArgsValidator.validate(enumSchema, "{\"unit\":\"k\"}");
        ToolArgsValidator.validate(SCHEMA, "{\"city\":\"北京\",\"days\":99}");
        var s = ToolArgsValidator.validationStats();
        assertThat(s.enumViolation()).isEqualTo(1);
        assertThat(s.rangeViolation()).isEqualTo(1);
    }

    @Test
    void nonCheckableSchemaDoesNotCount() {
        ToolArgsValidator.validate(null, "{}");
        ToolArgsValidator.validate("{}", "{\"a\":1}");
        var s = ToolArgsValidator.validationStats();
        assertThat(s.validations()).isZero();
    }

    @Test
    void resetForTestClearsAllCounters() {
        ToolArgsValidator.validate(SCHEMA, "{}");
        assertThat(ToolArgsValidator.validationStats().validations()).isEqualTo(1);
        ToolArgsValidator.resetValidationStatsForTest();
        var s = ToolArgsValidator.validationStats();
        assertThat(s.validations()).isZero();
        assertThat(s.accepted()).isZero();
        assertThat(s.missingRequired()).isZero();
    }
}
