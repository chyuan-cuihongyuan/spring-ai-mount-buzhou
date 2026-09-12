package io.github.chyuan_cuihongyuan.buzhou.starter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 快照 diff 破坏性分级测试（spec 706 / T963–T964 / impl 509）：added=非破坏、
 * removed=破坏、混合双侧各自分级、breaking 只由 removed 驱动。
 */
class ApiSnapshotDiffGradingTest {

    private static final String A = "buzhou-core|io.github.chyuan_cuihongyuan.buzhou.core.Foo";
    private static final String B = "buzhou-core|io.github.chyuan_cuihongyuan.buzhou.core.Bar";

    @Test
    void pureAdditionIsNonBreaking() {
        ApiSurfaceSnapshotTest.SnapshotDiff diff = ApiSurfaceSnapshotTest.gradeDiff(
                List.of(A), List.of(A, B));

        assertThat(diff.breaking()).isFalse();
        assertThat(diff.added()).containsExactly(B);
        assertThat(diff.removed()).isEmpty();
        assertThat(diff.gradeMessage()).contains("非破坏").contains("1");
    }

    @Test
    void pureRemovalIsBreaking() {
        ApiSurfaceSnapshotTest.SnapshotDiff diff = ApiSurfaceSnapshotTest.gradeDiff(
                List.of(A, B), List.of(A));

        assertThat(diff.breaking()).isTrue();
        assertThat(diff.removed()).containsExactly(B);
        assertThat(diff.gradeMessage()).contains("破坏性").contains("Bar");
    }

    @Test
    void mixedDiffGradesBothSides() {
        ApiSurfaceSnapshotTest.SnapshotDiff diff = ApiSurfaceSnapshotTest.gradeDiff(
                List.of(A), List.of(B));

        assertThat(diff.breaking()).isTrue();
        assertThat(diff.added()).containsExactly(B);
        assertThat(diff.removed()).containsExactly(A);
        assertThat(diff.gradeMessage()).contains("破坏性").contains("非破坏");
    }

    @Test
    void identicalSetsYieldEmptyDiff() {
        ApiSurfaceSnapshotTest.SnapshotDiff diff = ApiSurfaceSnapshotTest.gradeDiff(
                List.of(A, B), List.of(A, B));

        assertThat(diff.breaking()).isFalse();
        assertThat(diff.added()).isEmpty();
        assertThat(diff.removed()).isEmpty();
    }
}
