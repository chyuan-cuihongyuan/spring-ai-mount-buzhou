package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1868 / T2938：区间调度——合并（重叠/相邻/嵌套）、窗口缝隙。 */
class IntervalScheduleTest {

    /** 合并：重叠、相邻（9-10 + 10-11 = 9-11）、嵌套归一；乱序容忍。 */
    @Test
    void shouldMergeOverlappingTouchingAndNested() {
        List<IntervalSchedule.Interval> merged = IntervalSchedule.merge(List.of(
                new IntervalSchedule.Interval(10, 20),
                new IntervalSchedule.Interval(0, 10),   // 相邻
                new IntervalSchedule.Interval(15, 18),  // 嵌套
                new IntervalSchedule.Interval(30, 40))); // 独立
        assertThat(merged).containsExactly(
                new IntervalSchedule.Interval(0, 20),
                new IntervalSchedule.Interval(30, 40));
        assertThat(IntervalSchedule.merge(List.of())).isEmpty();
        assertThat(IntervalSchedule.merge(null)).isEmpty();
    }

    /** 缝隙：首前 + 区间间 + 尾后三段全报。 */
    @Test
    void shouldFindGapsAcrossWindow() {
        List<IntervalSchedule.Interval> gaps = IntervalSchedule.gaps(
                List.of(new IntervalSchedule.Interval(10, 20),
                        new IntervalSchedule.Interval(30, 40)),
                0, 50);
        assertThat(gaps).containsExactly(
                new IntervalSchedule.Interval(0, 10),
                new IntervalSchedule.Interval(20, 30),
                new IntervalSchedule.Interval(40, 50));
    }

    /** 全覆盖零缝隙；空占用全窗一缝；越界区间缝裁剪到窗口内。 */
    @Test
    void coverageAndClippingBehave() {
        assertThat(IntervalSchedule.gaps(
                List.of(new IntervalSchedule.Interval(0, 100)), 10, 90)).isEmpty();
        assertThat(IntervalSchedule.gaps(List.of(), 0, 30))
                .containsExactly(new IntervalSchedule.Interval(0, 30));
        // 区间越窗口界：只报窗口内缝
        assertThat(IntervalSchedule.gaps(
                List.of(new IntervalSchedule.Interval(-50, 15),
                        new IntervalSchedule.Interval(45, 200)), 0, 30))
                .containsExactly(new IntervalSchedule.Interval(15, 30));
    }

    /** 畸形入参 fail-fast：倒置区间、倒置窗口、null 区间。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new IntervalSchedule.Interval(10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法区间");
        assertThatThrownBy(() -> IntervalSchedule.gaps(List.of(), 10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法窗口");
        assertThatThrownBy(() -> IntervalSchedule.merge(
                java.util.Arrays.asList(new IntervalSchedule.Interval(0, 1), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("区间不能为 null");
    }
}
