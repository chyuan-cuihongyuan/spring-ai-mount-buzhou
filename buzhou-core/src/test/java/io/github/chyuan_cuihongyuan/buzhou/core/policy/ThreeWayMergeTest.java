package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.ThreeWayMerge.Outcome;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4042 / T6086：三方合并合同——单侧取侧、双侧不同区自动
 * 合、同改归一、同区异改冲突、同位追加冲突、确定性回放。
 */
class ThreeWayMergeTest {

    @Test
    void singleSideChangeShouldTakeThatSide() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a", "b", "c"), List.of("a", "X", "c"), List.of("a", "b", "c"));
        assertThat(outcome.lines()).containsExactly("a", "X", "c");
        assertThat(outcome.conflicts()).isEmpty();
        Outcome mirrored = ThreeWayMerge.merge(
                List.of("a", "b", "c"), List.of("a", "b", "c"), List.of("a", "b", "Z"));
        assertThat(mirrored.lines()).containsExactly("a", "b", "Z");
        assertThat(mirrored.conflicts()).isEmpty();
    }

    @Test
    void differentRegionsShouldMergeCleanly() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a", "b", "c", "d", "e"),
                List.of("a", "B", "c", "d", "e"),
                List.of("a", "b", "c", "D", "e"));
        assertThat(outcome.lines()).containsExactly("a", "B", "c", "D", "e");
        assertThat(outcome.conflicts()).isEmpty();
    }

    @Test
    void identicalChangesShouldNormalizeWithoutConflict() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a", "b", "c"), List.of("a", "X", "c"), List.of("a", "X", "c"));
        assertThat(outcome.lines()).containsExactly("a", "X", "c");
        assertThat(outcome.conflicts()).isEmpty();
    }

    @Test
    void sameRegionDifferentEditsShouldConflict() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a", "b", "c"), List.of("a", "X", "c"), List.of("a", "Y", "c"));
        assertThat(outcome.conflicts()).hasSize(1);
        assertThat(outcome.conflicts().get(0).oursLines()).containsExactly("X");
        assertThat(outcome.conflicts().get(0).theirsLines()).containsExactly("Y");
        assertThat(outcome.lines()).containsExactly(
                "a", "<<<<<<< ours", "X", "=======", "Y", ">>>>>>> theirs", "c");
    }

    @Test
    void samePositionAppendsShouldConflictConservatively() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a"), List.of("a", "b"), List.of("a", "c"));
        assertThat(outcome.conflicts()).hasSize(1);   // 相邻保守口径
        assertThat(outcome.lines()).containsExactly(
                "a", "<<<<<<< ours", "b", "=======", "c", ">>>>>>> theirs");
    }

    @Test
    void deletionVersusKeepShouldTakeDeletion() {
        Outcome outcome = ThreeWayMerge.merge(
                List.of("a", "b", "c"), List.of("a", "c"), List.of("a", "b", "c"));
        assertThat(outcome.lines()).containsExactly("a", "c");
        assertThat(outcome.conflicts()).isEmpty();
    }

    @Test
    void sameInputShouldReplayIdentically() {
        List<String> base = List.of("a", "b", "c", "d");
        List<String> ours = List.of("a", "B1", "B2", "d");
        List<String> theirs = List.of("a", "T", "c", "d");
        Outcome first = ThreeWayMerge.merge(base, ours, theirs);
        Outcome second = ThreeWayMerge.merge(base, ours, theirs);
        assertThat(first.lines()).isEqualTo(second.lines());
        assertThat(first.conflicts()).isEqualTo(second.conflicts());
    }

    @Test
    void nullInputsShouldFailFast() {
        assertThatThrownBy(() -> ThreeWayMerge.merge(null, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
