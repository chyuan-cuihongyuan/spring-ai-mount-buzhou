package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1873 / T2948：松弛量——关键零浮动、非关键缓冲、环拒绝。 */
class ScheduleFloatTest {

    /** 关键链全零浮动；旁支有缓冲。a(10)→b(100)、a→c(5)→d(10)：b 链关键。 */
    @Test
    void criticalChainZeroFloatAndSideBranchBuffered() {
        Map<String, ScheduleFloat.TaskFloat> floats = ScheduleFloat.floats(
                List.of(new ScheduleFloat.Task("a", 10),
                        new ScheduleFloat.Task("b", 100),
                        new ScheduleFloat.Task("c", 5),
                        new ScheduleFloat.Task("d", 10)),
                List.of(new ScheduleFloat.Dependency("a", "b"),
                        new ScheduleFloat.Dependency("a", "c"),
                        new ScheduleFloat.Dependency("c", "d")));
        // 总工期 110（a+b）。c：ES=10、到汇距=dur[d]=10 → LS=110−10−5=95 → float=85
        assertThat(floats.get("a").floatMillis()).isZero();
        assertThat(floats.get("b").floatMillis()).isZero();
        assertThat(floats.get("c").floatMillis()).isEqualTo(85L);
        // d：ES=15，LS=110−0−10=100 → float=85
        assertThat(floats.get("d").floatMillis()).isEqualTo(85L);
        assertThat(floats.get("a").earliestStartMillis()).isZero();
        assertThat(floats.get("c").earliestStartMillis()).isEqualTo(10L);
    }

    /** 串行链全关键：每任务 float 0。 */
    @Test
    void serialChainAllCritical() {
        Map<String, ScheduleFloat.TaskFloat> floats = ScheduleFloat.floats(
                List.of(new ScheduleFloat.Task("a", 10),
                        new ScheduleFloat.Task("b", 20)),
                List.of(new ScheduleFloat.Dependency("a", "b")));
        assertThat(floats.get("a").floatMillis()).isZero();
        assertThat(floats.get("b").floatMillis()).isZero();
    }

    /** 空输入零账；环与端点缺失与重复 fail-fast。 */
    @Test
    void emptyCycleAndMalformed() {
        assertThat(ScheduleFloat.floats(List.of(), List.of())).isEmpty();
        assertThatThrownBy(() -> ScheduleFloat.floats(
                List.of(new ScheduleFloat.Task("a", 1),
                        new ScheduleFloat.Task("b", 1)),
                List.of(new ScheduleFloat.Dependency("a", "b"),
                        new ScheduleFloat.Dependency("b", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("依赖图有环");
        assertThatThrownBy(() -> ScheduleFloat.floats(
                List.of(new ScheduleFloat.Task("a", 1)),
                List.of(new ScheduleFloat.Dependency("a", "ghost"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在任务集");
        assertThatThrownBy(() -> ScheduleFloat.floats(
                List.of(new ScheduleFloat.Task("a", 1),
                        new ScheduleFloat.Task("a", 2)), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务重复");
    }
}
