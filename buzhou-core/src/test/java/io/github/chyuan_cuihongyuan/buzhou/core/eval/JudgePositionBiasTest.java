package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1703 / T2608：JudgePositionBias 纯函数直测——镜像一致四桶归账与
 * 偏差比哨兵。
 */
class JudgePositionBiasTest {

    @Test
    void emptyCarriesNegativeSentinel() {
        var report = JudgePositionBias.analyze(List.of());
        assertThat(report.pairs()).isZero();
        assertThat(report.biasRatio()).isEqualTo(-1d);
    }

    @Test
    void mirroredConsistencyBuckets() {
        var report = JudgePositionBias.analyze(List.of(
                new JudgePositionBias.PairJudgement("c1",
                        JudgePositionBias.Verdict.A, JudgePositionBias.Verdict.B),
                new JudgePositionBias.PairJudgement("c2",
                        JudgePositionBias.Verdict.B, JudgePositionBias.Verdict.A),
                new JudgePositionBias.PairJudgement("c3",
                        JudgePositionBias.Verdict.TIE, JudgePositionBias.Verdict.TIE)));
        assertThat(report.consistent()).isEqualTo(3);
        assertThat(report.biasRatio()).isZero();
    }

    @Test
    void positionBiasBucketsAreCounted() {
        var report = JudgePositionBias.analyze(List.of(
                new JudgePositionBias.PairJudgement("first-bias",
                        JudgePositionBias.Verdict.A, JudgePositionBias.Verdict.A),
                new JudgePositionBias.PairJudgement("second-bias",
                        JudgePositionBias.Verdict.B, JudgePositionBias.Verdict.B),
                new JudgePositionBias.PairJudgement("mixed",
                        JudgePositionBias.Verdict.A, JudgePositionBias.Verdict.TIE),
                new JudgePositionBias.PairJudgement("ok",
                        JudgePositionBias.Verdict.A, JudgePositionBias.Verdict.B)));
        assertThat(report.pairs()).isEqualTo(4);
        assertThat(report.firstWinsBoth()).isEqualTo(1);
        assertThat(report.secondWinsBoth()).isEqualTo(1);
        assertThat(report.mixedTie()).isEqualTo(1);
        assertThat(report.consistent()).isEqualTo(1);
        assertThat(report.biasRatio()).isCloseTo(0.5d, within(1e-9));
    }

    @Test
    void nullInputTreatedAsEmpty() {
        var report = JudgePositionBias.analyze(null);
        assertThat(report.pairs()).isZero();
        assertThat(report.biasRatio()).isEqualTo(-1d);
    }
}
