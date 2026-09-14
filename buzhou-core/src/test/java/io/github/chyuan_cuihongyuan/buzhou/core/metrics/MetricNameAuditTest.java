package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1431 / T2166：指标命名校验器——与 MetricNamingGuardTest 门规则同源
 * 判定（合规样例/大写段/双点/尾空格/前缀缺失）、首违不短路一次看全、
 * 空名哨兵。
 */
class MetricNameAuditTest {

    @Test
    void canonicalNamesAreCompliant() {
        assertThat(MetricNameAudit.validate("buzhou.tool.calls").compliant()).isTrue();
        assertThat(MetricNameAudit.validate("buzhou.archive.pdb-rejected").compliant()).isTrue();
        assertThat(MetricNameAudit.validate("buzhou.bulkhead.scaling.scale-up").compliant()).isTrue();
        assertThat(MetricNameAudit.validate("buzhou.compaction").compliant()).isTrue();
    }

    @Test
    void upperCaseAndWhitespaceAreViolations() {
        var upper = MetricNameAudit.validate("buzhou.Tool.Calls");
        assertThat(upper.compliant()).isFalse();
        assertThat(upper.violations()).contains("SEGMENT_CASE:Tool");
        var trailing = MetricNameAudit.validate("buzhou.tool.calls ");
        assertThat(trailing.violations()).contains("WHITESPACE");
    }

    @Test
    void doubleDotProducesEmptySegment() {
        var v = MetricNameAudit.validate("buzhou.tool..calls");
        assertThat(v.compliant()).isFalse();
        assertThat(v.violations()).contains("SEGMENT_EMPTY");
    }

    @Test
    void prefixViolationForForeignFamily() {
        var v = MetricNameAudit.validate("jvm.memory.used");
        assertThat(v.compliant()).isFalse();
        assertThat(v.violations()).contains("PREFIX");
    }

    @Test
    void multipleViolationsReportedAtOnce() {
        // 首违不短路：大写段+双点+空段一次看全
        var v = MetricNameAudit.validate("buzhou.Tool..calls ");
        assertThat(v.violations()).contains(
                "WHITESPACE", "SEGMENT_CASE:Tool", "SEGMENT_EMPTY");
    }

    @Test
    void blankNameIsEmptyViolation() {
        var v = MetricNameAudit.validate(null);
        assertThat(v.violations()).containsExactly("EMPTY");
        assertThat(MetricNameAudit.validate("").violations()).containsExactly("EMPTY");
    }
}
