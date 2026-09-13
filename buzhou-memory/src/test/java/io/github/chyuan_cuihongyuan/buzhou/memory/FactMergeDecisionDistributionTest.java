package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 829 / T1160：合并决策分布回归——三分计数/替换率/段行声明序/只有触碰段出现/null 忽略/空真。
 */
class FactMergeDecisionDistributionTest {

    @Test
    void countsRatioAndSectionRows() {
        FactMergeDecisionDistribution dist = new FactMergeDecisionDistribution();
        dist.record(SummarySection.USER_INTENT, FactMergeDecisionDistribution.Decision.SUPERSEDED);
        dist.record(SummarySection.USER_INTENT, FactMergeDecisionDistribution.Decision.KEPT);
        dist.record(SummarySection.NEXT_STEP, FactMergeDecisionDistribution.Decision.CREATED);
        dist.record(SummarySection.PENDING_TASKS, FactMergeDecisionDistribution.Decision.SUPERSEDED);

        FactMergeDecisionDistribution.Report report = dist.snapshot();
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.kept()).isEqualTo(1);
        assertThat(report.superseded()).isEqualTo(2);
        assertThat(report.supersededRatio()).isCloseTo(0.5, within(1e-9));
        assertThat(report.sections()).hasSize(3);
        // 段行三分对账
        assertThat(report.sections().get(0).section()).isEqualTo(SummarySection.USER_INTENT);
        assertThat(report.sections().get(0).superseded()).isEqualTo(1);
        assertThat(report.sections().get(0).kept()).isEqualTo(1);
    }

    @Test
    void untouchedSectionsAbsentDeclarationOrderKept() {
        FactMergeDecisionDistribution dist = new FactMergeDecisionDistribution();
        dist.record(SummarySection.PENDING_TASKS, FactMergeDecisionDistribution.Decision.KEPT);
        dist.record(SummarySection.USER_INTENT, FactMergeDecisionDistribution.Decision.KEPT);
        // 声明序：USER_INTENT 先于 PENDING_TASKS
        assertThat(dist.snapshot().sections().get(0).section()).isEqualTo(SummarySection.USER_INTENT);
        assertThat(dist.snapshot().sections()).hasSize(2);
    }

    @Test
    void nullIgnoredAndEmptyTruth() {
        FactMergeDecisionDistribution dist = new FactMergeDecisionDistribution();
        dist.record(null, FactMergeDecisionDistribution.Decision.KEPT);
        dist.record(SummarySection.USER_INTENT, null);

        FactMergeDecisionDistribution.Report report = dist.snapshot();
        assertThat(report.total()).isZero();
        assertThat(report.sections()).isEmpty();
        assertThat(report.supersededRatio()).isZero();
        assertThat(report.created()).isZero();
    }
}
