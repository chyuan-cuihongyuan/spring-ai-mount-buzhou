package io.github.chyuan_cuihongyuan.buzhou.guard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1874 / T2950：正则风险——嵌套量词、交替形态、分级。 */
class RegexRiskAuditTest {

    /** 经典 ReDoS 嵌套量词 (a+)+ → 多形态 DANGEROUS（嵌套+量词组交替不中、重叠不中→SUSPECT 档）。 */
    @Test
    void nestedQuantifierIsFlagged() {
        RegexRiskAudit.Audit audit = RegexRiskAudit.audit("(a+)+");
        assertThat(audit.risk()).isEqualTo(RegexRiskAudit.Risk.SUSPECT);
        assertThat(audit.findings()).anyMatch(f -> f.contains("嵌套量词"));

        // 叠加形态：(a|ab)+ 内含交替又含嵌套风险 → DANGEROUS
        RegexRiskAudit.Audit stacked = RegexRiskAudit.audit("(a|ab+)+");
        assertThat(stacked.risk()).isEqualTo(RegexRiskAudit.Risk.DANGEROUS);
        assertThat(stacked.findings()).hasSize(3);
    }

    /** 量词组含交替 (a|b)* 与重叠交替 (a|ab) 各有形态命中。 */
    @Test
    void alternationFormsAreFlagged() {
        assertThat(RegexRiskAudit.audit("(a|b)*").risk())
                .isEqualTo(RegexRiskAudit.Risk.SUSPECT);
        assertThat(RegexRiskAudit.audit("(ab|a)*").findings())
                .anyMatch(f -> f.contains("交替"));
    }

    /** 安全模式：线性模式、字符类、转义量词不误报。 */
    @Test
    void safePatternsReadSafe() {
        for (String safe : new String[] {"^\\d{4}-\\d{2}$", "abc", "a+b+c",
                "[a-z]+@[a-z]+\\.[a-z]{2,}", "", "a\\+b"}) {
            assertThat(RegexRiskAudit.audit(safe).risk())
                    .as("模式 %s 应 SAFE", safe)
                    .isEqualTo(RegexRiskAudit.Risk.SAFE);
        }
    }

    /** 畸形入参 fail-fast：null 模式。 */
    @Test
    void nullPatternFailsFast() {
        assertThatThrownBy(() -> RegexRiskAudit.audit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern 不能为 null");
    }
}
