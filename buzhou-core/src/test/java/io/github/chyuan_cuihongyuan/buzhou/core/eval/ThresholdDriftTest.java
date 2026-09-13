package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-690 / spec 938：gate 阈值漂移读面——相邻同阈值 0 次、不同阈值精确计数、
 * 空/单条返回 0、纯函数零副作用。
 */
class ThresholdDriftTest {

    private static EvalGate.GateDecision decision(double threshold) {
        return new EvalGate.GateDecision(Instant.EPOCH, "ds", "r", threshold, 0.8, true);
    }

    @Test
    void sameThresholdZeroTransitions() {
        EvalGate.ThresholdDrift drift = EvalGate.thresholdDrift(
                List.of(decision(0.5), decision(0.5), decision(0.5)));
        assertThat(drift.transitions()).isZero();
        assertThat(drift.sampled()).isEqualTo(2);
    }

    @Test
    void alternatingThresholdsCountedExactly() {
        EvalGate.ThresholdDrift drift = EvalGate.thresholdDrift(
                List.of(decision(0.5), decision(0.7), decision(0.5)));
        assertThat(drift.transitions()).isEqualTo(2);
        assertThat(drift.sampled()).isEqualTo(2);
    }

    @Test
    void emptyAndSingleReturnZero() {
        assertThat(EvalGate.thresholdDrift(List.of()).transitions()).isZero();
        EvalGate.ThresholdDrift single = EvalGate.thresholdDrift(List.of(decision(0.5)));
        assertThat(single.transitions()).isZero();
        assertThat(single.sampled()).isZero();
    }
}
