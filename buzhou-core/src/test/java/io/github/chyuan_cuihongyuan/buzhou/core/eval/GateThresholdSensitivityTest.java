package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1437 兄弟轮 / T2186：门阈值敏感性扫描——带内计数、上调/下调翻转
 * 分向、敏感率派生、边界归属（左闭右开）、δ 负值 fail-fast、空集哨兵。
 */
class GateThresholdSensitivityTest {

    @Test
    void bandCountsSplitByFlipDirection() {
        // threshold=0.7, δ=0.05：带 [0.65, 0.75)
        var r = GateThresholdSensitivity.analyze(
                List.of(0.68, 0.72, 0.90, 0.10, 0.66), 0.7, 0.05);
        assertThat(r.totalScores()).isEqualTo(5);
        assertThat(r.bandCount()).isEqualTo(3); // 0.68 / 0.72 / 0.66
        assertThat(r.tightenFlips()).isEqualTo(1); // 0.72 当前 PASS → 上调翻 FAIL
        assertThat(r.loosenFlips()).isEqualTo(2); // 0.68 / 0.66 当前 FAIL → 下调翻 PASS
        assertThat(r.sensitivityRatio()).isEqualTo(3.0d / 5);
    }

    @Test
    void bandBoundariesAreHalfOpen() {
        var r = GateThresholdSensitivity.analyze(
                List.of(0.65, 0.75, 0.64, 0.75 + 1e-9), 0.7, 0.05);
        // [0.65, 0.75)：下界含端点（0.65 在带内）；0.75 恰出上界、0.750..01 出界
        assertThat(r.bandCount()).isEqualTo(1);
    }

    @Test
    void farScoresAreRobust() {
        var r = GateThresholdSensitivity.analyze(
                List.of(0.1, 0.2, 0.95, 0.99), 0.7, 0.05);
        assertThat(r.bandCount()).isZero();
        assertThat(r.sensitivityRatio()).isZero();
    }

    @Test
    void negativeDeltaFailsFast() {
        assertThatThrownBy(() -> GateThresholdSensitivity.analyze(List.of(0.5), 0.7, -0.01))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyScoresYieldZero() {
        var r = GateThresholdSensitivity.analyze(List.of(), 0.7, 0.05);
        assertThat(r.totalScores()).isZero();
        assertThat(r.sensitivityRatio()).isEqualTo(-1d);
    }
}
