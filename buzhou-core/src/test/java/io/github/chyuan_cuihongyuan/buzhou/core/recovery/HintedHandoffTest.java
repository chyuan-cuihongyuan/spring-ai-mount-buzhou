package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5010 / T6122：Hinted Handoff 合同——路由分叉、FIFO 回放、
 * 独立累积、状态机 IAE、未知目标 fail-fast。
 */
class HintedHandoffTest {

    @Test
    void healthyTargetShouldRouteNormally() {
        HintedHandoff handoff = new HintedHandoff();
        handoff.registerTarget("node-a");
        assertThat(handoff.route("node-a", "p1")).isFalse();   // 正常投递——无 hint
        assertThat(handoff.hintCountOf("node-a")).isZero();
        assertThat(handoff.isDown("node-a")).isFalse();
    }

    @Test
    void downTargetShouldAccumulateHintsInFifo() {
        HintedHandoff handoff = new HintedHandoff();
        handoff.registerTarget("node-a");
        handoff.markDown("node-a");
        assertThat(handoff.route("node-a", "p1")).isTrue();
        assertThat(handoff.route("node-a", "p2")).isTrue();
        assertThat(handoff.route("node-a", "p3")).isTrue();
        assertThat(handoff.hintCountOf("node-a")).isEqualTo(3);
    }

    @Test
    void markUpShouldReplayFifoAndClearQueue() {
        HintedHandoff handoff = new HintedHandoff();
        handoff.registerTarget("node-a");
        handoff.markDown("node-a");
        handoff.route("node-a", "p1");
        handoff.route("node-a", "p2");
        handoff.route("node-a", "p3");
        List<String> replayed = handoff.markUp("node-a");
        assertThat(replayed).containsExactly("p1", "p2", "p3");   // FIFO 回放
        assertThat(handoff.hintCountOf("node-a")).isZero();       // 回放清队
        assertThat(handoff.isDown("node-a")).isFalse();
        assertThat(handoff.route("node-a", "p4")).isFalse();      // 恢复正常路径
    }

    @Test
    void reDownShouldAccumulateIndependently() {
        HintedHandoff handoff = new HintedHandoff();
        handoff.registerTarget("node-a");
        handoff.markDown("node-a");
        handoff.route("node-a", "first-round");
        List<String> first = handoff.markUp("node-a");
        assertThat(first).containsExactly("first-round");
        handoff.markDown("node-a");
        handoff.route("node-a", "second-round");
        List<String> second = handoff.markUp("node-a");
        assertThat(second).containsExactly("second-round");   // 新 hint 独立
    }

    @Test
    void duplicateTransitionsAndUnknownTargetShouldFailFast() {
        HintedHandoff handoff = new HintedHandoff();
        handoff.registerTarget("node-a");
        handoff.markDown("node-a");
        assertThatThrownBy(() -> handoff.markDown("node-a"))
                .isInstanceOf(IllegalArgumentException.class);   // 重复下线
        assertThatThrownBy(() -> handoff.registerTarget("node-a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handoff.route("ghost", "p"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handoff.markUp("node-b"))
                .isInstanceOf(IllegalArgumentException.class);
        HintedHandoff fresh = new HintedHandoff();
        fresh.registerTarget("up");
        assertThatThrownBy(() -> fresh.markUp("up"))
                .isInstanceOf(IllegalArgumentException.class);   // 未下线不可恢复
    }
}
