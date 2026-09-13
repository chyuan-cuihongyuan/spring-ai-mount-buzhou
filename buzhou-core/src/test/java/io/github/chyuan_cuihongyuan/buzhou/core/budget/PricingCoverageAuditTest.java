package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 827 / T1156：定价覆盖审计回归——精确/忽略大小写/provider 前缀剥离/
 * 去重与脏名/覆盖率/未知典序封顶/空真。
 */
class PricingCoverageAuditTest {

    @Test
    void matchingSemanticsLayered() {
        var report = PricingCoverageAudit.audit(
                Set.of("gpt-4o", "claude-3-5-sonnet"),
                List.of("gpt-4o", "GPT-4O", "openai/gpt-4o", "mystery-model", "mystery-model"));

        assertThat(report.calledModels()).isEqualTo(4); // 去重后
        assertThat(report.coveredModels()).isEqualTo(3); // 精确+大小写+前缀剥离
        assertThat(report.coverageRatio()).isCloseTo(0.75, within(1e-9));
        assertThat(report.unknown()).containsExactly("mystery-model");
    }

    @Test
    void unknownListSortedDedupedAndCapped() {
        Set<String> priced = Set.of("known");
        List<String> called = new java.util.ArrayList<>();
        for (int i = 0; i < PricingCoverageAudit.MAX_LIST + 5; i++) {
            called.add("unk-" + (PricingCoverageAudit.MAX_LIST - i)); // 乱序
        }
        called.add("known");
        var report = PricingCoverageAudit.audit(priced, called);

        assertThat(report.unknown()).hasSize(PricingCoverageAudit.MAX_LIST + 1); // 封顶+汇总行
        assertThat(report.unknown().get(PricingCoverageAudit.MAX_LIST)).startsWith("…（共");
        assertThat(report.unknown().subList(0, PricingCoverageAudit.MAX_LIST))
                .isSorted(); // 典序
        assertThat(report.coveredModels()).isEqualTo(1);
    }

    @Test
    void fullCoverageRatioIsOne() {
        var report = PricingCoverageAudit.audit(Set.of("a", "b"), List.of("a", "b", "A", "x/b"));
        assertThat(report.coverageRatio()).isCloseTo(1.0, within(1e-9));
        assertThat(report.unknown()).isEmpty();
    }

    @Test
    void emptyCalledAndDirtyNames() {
        var empty = PricingCoverageAudit.audit(Set.of("a"), List.of());
        assertThat(empty.calledModels()).isZero();
        assertThat(empty.coverageRatio()).isEqualTo(1.0); // 空调用=无缺口（空真）

        var dirty = PricingCoverageAudit.audit(Set.of("a"),
                java.util.Arrays.asList(null, "", "  ", "a"));
        assertThat(dirty.calledModels()).isEqualTo(1);
        assertThat(dirty.coveredModels()).isEqualTo(1);
    }

    @Test
    void emptyPricedTableEverythingUnknown() {
        var report = PricingCoverageAudit.audit(Set.of(), List.of("x", "y"));
        assertThat(report.coveredModels()).isZero();
        assertThat(report.coverageRatio()).isZero();
        assertThat(report.unknown()).containsExactly("x", "y");
    }
}
