package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1815 / T2832：缺口回填——区间合并、边界缺口、完整性读数。 */
class GapBackfillPlannerTest {

    /** 中段缺口合并为区间：0-9 在位缺 3-5 与 8 → 两段缺口升序。 */
    @Test
    void shouldMergeMiddleGapsIntoRanges() {
        GapBackfillPlanner.BackfillPlan plan = GapBackfillPlanner.plan(0, 9,
                List.of(0L, 1L, 2L, 6L, 7L, 9L));
        assertThat(plan.expectedCount()).isEqualTo(10L);
        assertThat(plan.presentCount()).isEqualTo(6L);
        assertThat(plan.gaps()).hasSize(2);
        assertThat(plan.gaps().get(0).fromInclusive()).isEqualTo(3L);
        assertThat(plan.gaps().get(0).toInclusive()).isEqualTo(5L);
        assertThat(plan.gaps().get(1).fromInclusive()).isEqualTo(8L);
        assertThat(plan.gaps().get(1).toInclusive()).isEqualTo(8L);
        assertThat(plan.largestGapSpan()).isEqualTo(3L);
        assertThat(plan.missingRatio()).isEqualTo(0.4d);
        assertThat(plan.complete()).isFalse();
    }

    /** 首尾缺口也入账：重放边界不漏。 */
    @Test
    void edgeGapsCounted() {
        GapBackfillPlanner.BackfillPlan plan = GapBackfillPlanner.plan(0, 9,
                List.of(4L, 5L));
        assertThat(plan.gaps()).hasSize(2);
        assertThat(plan.gaps().get(0)).isEqualTo(new GapBackfillPlanner.Gap(0, 3));
        assertThat(plan.gaps().get(1)).isEqualTo(new GapBackfillPlanner.Gap(6, 9));
        assertThat(plan.largestGapSpan()).isEqualTo(4L);
    }

    /** 全在位完整；空集全缺；乱序重复容忍。 */
    @Test
    void completeEmptyAndDirtyInput() {
        GapBackfillPlanner.BackfillPlan complete = GapBackfillPlanner.plan(0, 4,
                List.of(0L, 1L, 2L, 3L, 4L));
        assertThat(complete.complete()).isTrue();
        assertThat(complete.missingRatio()).isZero();

        GapBackfillPlanner.BackfillPlan none = GapBackfillPlanner.plan(0, 4, List.of());
        assertThat(none.gaps()).hasSize(1);
        assertThat(none.missingRatio()).isEqualTo(1.0d);

        GapBackfillPlanner.BackfillPlan dirty = GapBackfillPlanner.plan(0, 4,
                List.of(3L, 1L, 1L, 0L, 4L, 2L));
        assertThat(dirty.complete()).isTrue();
        assertThat(dirty.presentCount()).isEqualTo(5L);
    }

    /** 空区间（from = to+1）哨兵：期望 0、完整、比率 -1。 */
    @Test
    void emptyRangeYieldsSentinel() {
        GapBackfillPlanner.BackfillPlan plan = GapBackfillPlanner.plan(5, 4, null);
        assertThat(plan.expectedCount()).isZero();
        assertThat(plan.complete()).isTrue();
        assertThat(plan.missingRatio()).isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：区间倒挂、越界在位值、null 元素。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> GapBackfillPlanner.plan(10, 5, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from ≤ to+1");
        assertThatThrownBy(() -> GapBackfillPlanner.plan(0, 9, List.of(10L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> GapBackfillPlanner.plan(0, 9,
                java.util.Arrays.asList(1L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为 null");
    }
}
