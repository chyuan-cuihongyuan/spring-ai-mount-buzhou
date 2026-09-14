package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1421 / T2144：实验分桶均衡审计——A/A 均衡判定、零桶失衡、
 * 份额降序、无样本哨兵、容差边界。
 */
class ExperimentBalanceAuditTest {

    @Test
    void perfectlyEvenSplitIsBalanced() {
        var report = ExperimentBalanceAudit.forBuckets(List.of("a", "b"))
                .withAssignments(Map.of("a", 500L, "b", 500L));
        assertThat(report.balanced()).isTrue();
        assertThat(report.maxDeviation()).isEqualTo(0.0d, within(1e-9));
        assertThat(report.totalAssignments()).isEqualTo(1000);
    }

    @Test
    void skewedSplitBeyondToleranceIsUnbalanced() {
        // 90/10：均匀 50%，最大偏移 40pp > 5pp 容差
        var report = ExperimentBalanceAudit.forBuckets(List.of("a", "b"))
                .withAssignments(Map.of("a", 900L, "b", 100L));
        assertThat(report.balanced()).isFalse();
        assertThat(report.maxDeviation()).isEqualTo(0.4d, within(1e-9));
        // 最紧桶排序：assignments 降序 a 在前
        assertThat(report.buckets().get(0).bucket()).isEqualTo("a");
    }

    @Test
    void missingBucketCountsAsZeroAndFailsAudit() {
        // 声明三桶只喂两桶——零分配桶是最典型权重配置错误
        var report = ExperimentBalanceAudit.forBuckets(List.of("a", "b", "c"))
                .withAssignments(Map.of("a", 600L, "b", 400L));
        assertThat(report.buckets()).hasSize(3); // 零桶仍在列
        assertThat(report.buckets().stream()
                .filter(s -> s.bucket().equals("c")).findFirst().orElseThrow()
                .assignments()).isZero();
        // 零桶份额偏移 1/3 > 5pp → 失衡
        assertThat(report.balanced()).isFalse();
    }

    @Test
    void emptyAssignmentsReportSentinel() {
        var report = ExperimentBalanceAudit.forBuckets(List.of("a", "b"))
                .withAssignments(Map.of("a", 0L, "b", 0L));
        assertThat(report.maxDeviation()).isEqualTo(-1d); // 无样本哨兵
        assertThat(report.balanced()).isFalse(); // 不冒充均衡
        assertThat(report.totalAssignments()).isZero();
    }

    @Test
    void toleranceBoundaryIsInclusive() {
        // 恰好 5pp 偏移（52.5/47.5 二桶）：52.5%−50%=2.5pp？改三桶精确构造：
        // 三桶均匀 1/3≈33.33pp；构造 38.33/33.33/28.33 → 最大偏移 5pp 恰在容差内
        var report = ExperimentBalanceAudit.forBuckets(List.of("a", "b", "c"))
                .withAssignments(Map.of("a", 1150L, "b", 1000L, "c", 850L));
        // total=3000：份额 38.33/33.33/28.33 —— 偏移恰 5pp → 含端点判均衡
        assertThat(report.maxDeviation()).isCloseTo(0.05d, within(1e-9));
        assertThat(report.balanced()).isTrue();
    }
}
