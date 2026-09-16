package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2013 / T3128：启动豁免窗合同——窗内失败豁免、窗外计账、毕业
 * 幂等、毕业后全额计账、未锚定计账、多实例独立、畸形 fail-fast。
 */
class StartupGraceTrackerTest {

    @Test
    void failureInsideGraceWindowShouldBeExempted() {
        StartupGraceTracker tracker = new StartupGraceTracker(10_000L);
        tracker.begin("tool-mcp", 0);
        tracker.reportFailure("tool-mcp", 5_000L); // 窗内
        tracker.reportFailure("tool-mcp", 9_999L); // 界内（<grace）
        assertThat(tracker.stats().exemptedFailures()).isEqualTo(2L);
        assertThat(tracker.stats().countedFailures()).isZero();
        assertThat(tracker.activeGraces(6_000L)).isEqualTo(1);
    }

    @Test
    void failureOutsideWindowShouldCount() {
        StartupGraceTracker tracker = new StartupGraceTracker(10_000L);
        tracker.begin("tool-mcp", 0);
        tracker.reportFailure("tool-mcp", 10_000L); // 恰到期（>=grace）→ 计账
        assertThat(tracker.stats().countedFailures()).isEqualTo(1L);
        assertThat(tracker.stats().exemptedFailures()).isZero();
    }

    @Test
    void graduationShouldEndExemptionIdempotently() {
        StartupGraceTracker tracker = new StartupGraceTracker(10_000L);
        tracker.begin("i", 0);
        tracker.graduate("i"); // 首次成功毕业
        tracker.graduate("i"); // 幂等
        tracker.reportFailure("i", 1_000L); // 毕业后窗内也计账
        assertThat(tracker.stats().graduations()).isEqualTo(1L);
        assertThat(tracker.stats().countedFailures()).isEqualTo(1L);
        assertThat(tracker.stats().exemptedFailures()).isZero();
    }

    @Test
    void unanchoredInstanceShouldAlwaysCount() {
        StartupGraceTracker tracker = new StartupGraceTracker(10_000L);
        tracker.reportFailure("never-began", 0); // 无豁免资格
        assertThat(tracker.stats().countedFailures()).isEqualTo(1L);
    }

    @Test
    void rebeginShouldRestartGraceWindow() {
        StartupGraceTracker tracker = new StartupGraceTracker(1_000L);
        tracker.begin("i", 0);
        tracker.reportFailure("i", 500); // 窗内豁免
        tracker.begin("i", 5_000); // 重启重锚
        tracker.reportFailure("i", 5_500); // 新窗内豁免
        assertThat(tracker.stats().exemptedFailures()).isEqualTo(2L);
        tracker.reportFailure("i", 7_000); // 新窗外计账
        assertThat(tracker.stats().countedFailures()).isEqualTo(1L);
    }

    @Test
    void instancesShouldTrackIndependently() {
        StartupGraceTracker tracker = new StartupGraceTracker(1_000L);
        tracker.begin("a", 0);
        tracker.begin("b", 500);
        tracker.graduate("a");
        tracker.reportFailure("a", 100); // 已毕业计账
        tracker.reportFailure("b", 900); // 窗内豁免
        assertThat(tracker.stats().countedFailures()).isEqualTo(1L);
        assertThat(tracker.stats().exemptedFailures()).isEqualTo(1L);
        assertThat(tracker.activeGraces(900)).isEqualTo(1); // 仅 b
    }

    @Test
    void malformedInputsShouldFailFast() {
        StartupGraceTracker tracker = new StartupGraceTracker(1_000L);
        assertThatThrownBy(() -> new StartupGraceTracker(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.begin(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.begin("i", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.reportFailure(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.graduate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
