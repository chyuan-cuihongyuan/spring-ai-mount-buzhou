package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 711 / T1022–T1023：消息序列连续性审计——健康零发现、GAP（turn 内
 * 断号/turn 缺号）、DUPLICATE、OUT_OF_ORDER、空表与 null。
 */
class TurnSequenceAuditTest {

    private static TurnSequenceAudit.Marker m(int turn, int seq) {
        return new TurnSequenceAudit.Marker(turn, seq);
    }

    @Test
    void healthySequenceHasNoFindings() {
        List<TurnSequenceAudit.Marker> markers = List.of(
                m(0, 0), m(0, 1), m(0, 2),
                m(1, 0), m(1, 1),
                m(2, 0));
        assertThat(TurnSequenceAudit.audit(markers)).isEmpty();
    }

    @Test
    void gapsWithinTurnAndMissingTurnsAreReported() {
        // turn 内断号：seq 0 → 2（缺 1）
        List<TurnSequenceAudit.Marker> withinTurn = List.of(m(0, 0), m(0, 2));
        assertThat(TurnSequenceAudit.audit(withinTurn))
                .containsExactly(new TurnSequenceAudit.Finding("GAP", 0, 2));

        // turn 缺号：turn1 整体缺失（0 → 2）
        List<TurnSequenceAudit.Marker> missingTurn = List.of(m(0, 0), m(2, 0));
        assertThat(TurnSequenceAudit.audit(missingTurn))
                .containsExactly(new TurnSequenceAudit.Finding("GAP", 2, 0));

        // 首对非 (0,0)：起始约定违反
        List<TurnSequenceAudit.Marker> badStart = List.of(m(1, 0));
        assertThat(TurnSequenceAudit.audit(badStart))
                .containsExactly(new TurnSequenceAudit.Finding("GAP", 1, 0));
    }

    @Test
    void duplicatesAndOutOfOrderAreReported() {
        List<TurnSequenceAudit.Marker> duplicated = new ArrayList<>();
        duplicated.add(m(0, 0));
        duplicated.add(m(0, 0)); // 重复
        duplicated.add(m(0, 1));
        assertThat(TurnSequenceAudit.audit(duplicated))
                .containsExactly(new TurnSequenceAudit.Finding("DUPLICATE", 0, 0));

        List<TurnSequenceAudit.Marker> outOfOrder = List.of(
                m(0, 0), m(0, 1), m(2, 0), m(1, 0)); // (1,0) 在 (2,0) 之后
        assertThat(TurnSequenceAudit.audit(outOfOrder))
                .contains(new TurnSequenceAudit.Finding("OUT_OF_ORDER", 1, 0));
    }

    @Test
    void emptyIsEmptyAndNullFailsFast() {
        assertThat(TurnSequenceAudit.audit(List.of())).isEmpty();
        assertThatThrownBy(() -> TurnSequenceAudit.audit(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TurnSequenceAudit.audit(new ArrayList<>() {{
            add(null);
        }})).isInstanceOf(NullPointerException.class);
    }
}
