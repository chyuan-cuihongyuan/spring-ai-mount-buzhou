package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1867 / T2936：关键路径——最长链、分支取长、环拒绝。 */
class CriticalPathLengthTest {

    /** 串行链：10+20+30=60，终点 c。 */
    @Test
    void chainSumsDurations() {
        CriticalPathLength.Result r = CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("a", 10),
                        new CriticalPathLength.Task("b", 20),
                        new CriticalPathLength.Task("c", 30)),
                List.of(new CriticalPathLength.Dependency("a", "b"),
                        new CriticalPathLength.Dependency("b", "c")));
        assertThat(r.criticalPathMillis()).isEqualTo(60L);
        assertThat(r.terminalTask()).isEqualTo("c");
    }

    /** 分支取长：a(10)→b(100) 与 a→c(5)→d(10)——关键走 a+b=110。 */
    @Test
    void parallelBranchTakesLongest() {
        CriticalPathLength.Result r = CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("a", 10),
                        new CriticalPathLength.Task("b", 100),
                        new CriticalPathLength.Task("c", 5),
                        new CriticalPathLength.Task("d", 10)),
                List.of(new CriticalPathLength.Dependency("a", "b"),
                        new CriticalPathLength.Dependency("a", "c"),
                        new CriticalPathLength.Dependency("c", "d")));
        assertThat(r.criticalPathMillis()).isEqualTo(110L);
        assertThat(r.terminalTask()).isEqualTo("b");
    }

    /** 单任务、空 DAG、非连通 DAG（各分量取最大）。 */
    @Test
    void degenerateAndDisconnectedGraphs() {
        assertThat(CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("solo", 7)), List.of())
                .criticalPathMillis()).isEqualTo(7L);
        assertThat(CriticalPathLength.longestPath(List.of(), List.of()).terminalTask())
                .isNull();
        CriticalPathLength.Result disconnected = CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("x", 3),
                        new CriticalPathLength.Task("y", 50)),
                List.of());
        assertThat(disconnected.criticalPathMillis()).isEqualTo(50L);
        assertThat(disconnected.terminalTask()).isEqualTo("y");
    }

    /** 环 fail-fast；端点缺任务 fail-fast。 */
    @Test
    void cyclesAndMissingEndpointsFailFast() {
        assertThatThrownBy(() -> CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("a", 1),
                        new CriticalPathLength.Task("b", 1)),
                List.of(new CriticalPathLength.Dependency("a", "b"),
                        new CriticalPathLength.Dependency("b", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("依赖图有环");
        assertThatThrownBy(() -> CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("a", 1)),
                List.of(new CriticalPathLength.Dependency("a", "ghost"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在任务集");
    }

    /** 畸形任务与依赖 fail-fast：空白 id、负时长、重复任务。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new CriticalPathLength.Task(" ", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法任务");
        assertThatThrownBy(() -> new CriticalPathLength.Task("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CriticalPathLength.longestPath(
                List.of(new CriticalPathLength.Task("a", 1),
                        new CriticalPathLength.Task("a", 2)), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务重复");
    }
}
