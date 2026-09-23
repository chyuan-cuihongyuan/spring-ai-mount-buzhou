package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.SpeculativeStragglerPolicy.TaskStats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4026 / T6054：推测执行合同——慢且落后才推测、进度豁免、
 * 不够慢豁免、恰界不推测、畸形 fail-fast。
 */
class SpeculativeStragglerPolicyTest {

    private static final List<TaskStats> PEERS = List.of(
            new TaskStats("p1", 100, 1.0),
            new TaskStats("p2", 200, 1.0),
            new TaskStats("p3", 300, 1.0));   // 中位 200

    @Test
    void slowAndBehindShouldSpeculate() {
        SpeculativeStragglerPolicy policy = new SpeculativeStragglerPolicy(1.5, 0.75);
        TaskStats straggler = new TaskStats("laggard", 400, 0.4);   // 400 > 200×1.5 且 0.4<0.75
        assertThat(policy.shouldSpeculate(straggler, PEERS)).isTrue();
        assertThat(policy.multiplier()).isEqualTo(1.5);
        assertThat(policy.progressThreshold()).isEqualTo(0.75);
    }

    @Test
    void nearCompletionShouldBeExempt() {
        SpeculativeStragglerPolicy policy = new SpeculativeStragglerPolicy(1.5, 0.75);
        TaskStats slowButDone = new TaskStats("almost", 400, 0.8);   // 慢但进度过阈——不折腾
        assertThat(policy.shouldSpeculate(slowButDone, PEERS)).isFalse();
    }

    @Test
    void notSlowEnoughShouldBeExempt() {
        SpeculativeStragglerPolicy policy = new SpeculativeStragglerPolicy(1.5, 0.75);
        assertThat(policy.shouldSpeculate(new TaskStats("ok", 290, 0.3), PEERS)).isFalse();   // 290 < 300
        assertThat(policy.shouldSpeculate(new TaskStats("edge", 300, 0.3), PEERS)).isFalse();   // 恰界不含
    }

    @Test
    void medianShouldResistOutliers() {
        SpeculativeStragglerPolicy policy = new SpeculativeStragglerPolicy(2.0, 0.75);
        List<TaskStats> withOutlier = List.of(
                new TaskStats("fast", 100, 1.0),
                new TaskStats("norm", 200, 1.0),
                new TaskStats("slow-done", 60_000, 1.0));   // 离群完成者——中位仍 200
        assertThat(SpeculativeStragglerPolicy.medianElapsed(withOutlier)).isEqualTo(200);
        assertThat(policy.shouldSpeculate(new TaskStats("lag", 500, 0.2), withOutlier)).isTrue();
        // 偶数同伴取下中位（确定性）
        assertThat(SpeculativeStragglerPolicy.medianElapsed(
                List.of(new TaskStats("a", 100, 1.0), new TaskStats("b", 400, 1.0)))).isEqualTo(100);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SpeculativeStragglerPolicy(0.9, 0.75))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeculativeStragglerPolicy(1.5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeculativeStragglerPolicy(1.5, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
        SpeculativeStragglerPolicy policy = new SpeculativeStragglerPolicy(1.5, 0.75);
        assertThatThrownBy(() -> policy.shouldSpeculate(null, PEERS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.shouldSpeculate(
                new TaskStats("x", -1, 0.5), PEERS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.shouldSpeculate(
                new TaskStats("x", 100, 1.5), PEERS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.shouldSpeculate(
                new TaskStats("x", 100, 0.5), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
