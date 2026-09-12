package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 731 / T1062–T1063：评估分数分布解析——similarity 分数提取/统计/
 * 无分数项跳过/null fail-fast。
 */
class EvalScoreAnalyticsTest {

    private static EvalRunResult runOf(String... details) {
        List<EvalRunItemResult> items = new java.util.ArrayList<>();
        for (int i = 0; i < details.length; i++) {
            items.add(new EvalRunItemResult("i" + i, "pass", details[i], null, 1));
        }
        return new EvalRunResult("r1", "suite", Instant.EPOCH, Instant.EPOCH,
                items.size(), items.size(), 0, 0, items, null);
    }

    @Test
    void parsesScoresAndComputesStats() {
        EvalRunResult run = runOf(
                "similarity=0.900000 阈值=0.8",
                "similarity=0.500000 阈值=0.8",
                "similarity=0.700000 阈值=0.8",
                "exact 命中"); // 无分数口径——跳过
        EvalScoreAnalytics.Report report = EvalScoreAnalytics.similarityScores(run);
        assertThat(report.scored()).isEqualTo(3);
        assertThat(report.min()).isEqualTo(0.5);
        assertThat(report.max()).isEqualTo(0.9);
        assertThat(report.mean()).isEqualTo(0.7, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(report.scores()).containsExactly(0.9, 0.5, 0.7);
    }

    @Test
    void noScoresAndNullFailFast() {
        EvalScoreAnalytics.Report none = EvalScoreAnalytics.similarityScores(runOf("exact 命中"));
        assertThat(none.scored()).isZero();
        assertThat(Double.isNaN(none.mean())).isTrue();
        assertThatThrownBy(() -> EvalScoreAnalytics.similarityScores(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
