package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1407 / T2116：PII 合成探针自查——正例召回可量化、负例零误报、
 * 报告典序与派生指标；探针只读（不改检测器状态）。
 */
class PiiProbeSelfCheckTest {

    @Test
    void probeReportsPerTypeRecall() {
        PiiProbeSelfCheck.ProbeReport report =
                PiiProbeSelfCheck.probe(new PiiDetector());
        assertThat(report.recalls()).isNotEmpty();
        // 报告按 PiiType 典序
        for (int i = 1; i < report.recalls().size(); i++) {
            assertThat(report.recalls().get(i - 1).type().ordinal())
                    .isLessThan(report.recalls().get(i).type().ordinal());
        }
        // 每类型 total 与正例池一致（EMAIL/CN_PHONE=2，其余=1 或 2——以池为准只断结构）
        for (PiiProbeSelfCheck.TypeRecall r : report.recalls()) {
            assertThat(r.total()).isPositive();
            assertThat(r.hits()).isBetween(0, r.total());
        }
    }

    @Test
    void baselineDetectorShouldRecallAllBuiltInSamples() {
        PiiProbeSelfCheck.ProbeReport report =
                PiiProbeSelfCheck.probe(new PiiDetector());
        // 内建正例池与内建检测器同步演化——基线应为全召回（回归哨兵的标定锚）
        assertThat(report.overallRecall()).isEqualTo(1.0d);
        for (PiiProbeSelfCheck.TypeRecall r : report.recalls()) {
            assertThat(r.hits()).as("类型 %s 召回", r.type()).isEqualTo(r.total());
        }
    }

    @Test
    void negativesMustProduceZeroFalsePositives() {
        PiiProbeSelfCheck.ProbeReport report =
                PiiProbeSelfCheck.probe(new PiiDetector());
        // 普通文本零误报——非零即检测器规则过宽（回归哨兵）
        assertThat(report.falsePositives()).isZero();
    }

    @Test
    void probeIsReadOnlyAndRepeatable() {
        PiiDetector detector = new PiiDetector();
        var first = PiiProbeSelfCheck.probe(detector);
        var second = PiiProbeSelfCheck.probe(detector);
        // 纯函数：同 detector 重复探针结果逐位一致
        assertThat(first).isEqualTo(second);
    }
}
