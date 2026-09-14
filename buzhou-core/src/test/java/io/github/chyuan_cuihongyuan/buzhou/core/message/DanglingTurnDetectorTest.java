package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1437 / T2178：悬空轮检测——有 USER 无 ASSISTANT 即悬空、TOOL 链
 * 不影响判定、仅 TOOL/SYSTEM 轮不算悬空、样本封顶升序、空输入哨兵。
 */
class DanglingTurnDetectorTest {

    private static BuzhouMessage msg(int turnSeq, Role role) {
        return new BuzhouMessage("id-" + turnSeq + role, "s", turnSeq, 0, role,
                "内容", List.of(), null, null, null, java.util.Map.of(), null);
    }

    @Test
    void emptyInputYieldsZero() {
        var r = DanglingTurnDetector.analyze(List.of());
        assertThat(r.totalTurns()).isZero();
        assertThat(r.danglingTurnCount()).isZero();
        assertThat(r.hasDangling()).isFalse();
    }

    @Test
    void answeredTurnIsNotDangling() {
        var r = DanglingTurnDetector.analyze(List.of(
                msg(1, Role.USER), msg(1, Role.ASSISTANT),
                msg(2, Role.USER), msg(2, Role.ASSISTANT)));
        assertThat(r.totalTurns()).isEqualTo(2);
        assertThat(r.danglingTurnCount()).isZero();
    }

    @Test
    void userTurnWithoutAssistantIsDangling() {
        var r = DanglingTurnDetector.analyze(List.of(
                msg(1, Role.USER), msg(1, Role.ASSISTANT),
                msg(2, Role.USER))); // 取消/中断残留
        assertThat(r.danglingTurnCount()).isEqualTo(1);
        assertThat(r.danglingSamples()).containsExactly(2);
        assertThat(r.hasDangling()).isTrue();
    }

    @Test
    void danglingTurnWithToolChainStillDangling() {
        // 工具链后无 ASSISTANT 收尾——同样悬空
        var r = DanglingTurnDetector.analyze(List.of(
                msg(3, Role.USER), msg(3, Role.TOOL)));
        assertThat(r.danglingTurnCount()).isEqualTo(1);
        assertThat(r.danglingSamples()).containsExactly(3);
    }

    @Test
    void toolAndSystemOnlyTurnsNotDangling() {
        var r = DanglingTurnDetector.analyze(List.of(
                msg(1, Role.TOOL), msg(2, Role.SYSTEM)));
        assertThat(r.danglingTurnCount()).isZero();
        assertThat(r.totalTurns()).isEqualTo(2);
    }

    @Test
    void samplesCappedAndAscending() {
        var msgs = new java.util.ArrayList<BuzhouMessage>();
        for (int t = 1; t <= 20; t++) {
            msgs.add(msg(t, Role.USER)); // 20 个悬空轮
        }
        var r = DanglingTurnDetector.analyze(msgs);
        assertThat(r.danglingTurnCount()).isEqualTo(20);
        assertThat(r.danglingSamples()).hasSize(8); // 封顶
        assertThat(r.danglingSamples()).isSorted();
    }
}
