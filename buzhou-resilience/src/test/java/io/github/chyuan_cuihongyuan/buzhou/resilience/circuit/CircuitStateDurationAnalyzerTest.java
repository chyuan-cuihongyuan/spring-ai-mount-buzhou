package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1408 / T2118：断路器状态时长分析器——积段积分、末段延伸至 now、
 * 无序容忍、OPEN 占比、多模型分组、空输入哨兵。
 */
class CircuitStateDurationAnalyzerTest {

    private static CircuitTransitionJournal.Transition t(
            String model, String from, String to, long atMs) {
        return new CircuitTransitionJournal.Transition(model, from, to, atMs, 1, 0);
    }

    @Test
    void emptyTransitionsYieldEmptyReport() {
        assertThat(CircuitStateDurationAnalyzer.analyze(List.of(), 1000L)).isEmpty();
        assertThat(CircuitStateDurationAnalyzer.analyze(null, 1000L)).isEmpty();
    }

    @Test
    void segmentsIntegrateIntoStateDurations() {
        // CLOSED 0→100，OPEN 100→300，CLOSED 300→500（now=500）
        List<CircuitTransitionJournal.Transition> transitions = List.of(
                t("m", "CLOSED", "OPEN", 100),
                t("m", "OPEN", "CLOSED", 300));
        // 单条起始变迁不带起始状态段——窗口自首条变迁起（观测窗 400ms）
        var report = CircuitStateDurationAnalyzer.analyze(transitions, 500L);
        assertThat(report).hasSize(1);
        var md = report.get(0);
        assertThat(md.model()).isEqualTo("m");
        assertThat(md.observedMillis()).isEqualTo(400);
        // OPEN 100→300 = 200ms 一段；CLOSED 300→500 = 200ms 一段
        assertThat(md.states()).hasSize(2);
        var open = md.states().stream()
                .filter(s -> s.state().equals("OPEN")).findFirst().orElseThrow();
        assertThat(open.totalMillis()).isEqualTo(200);
        assertThat(open.segments()).isEqualTo(1);
        assertThat(open.maxMillis()).isEqualTo(200);
        var closed = md.states().stream()
                .filter(s -> s.state().equals("CLOSED")).findFirst().orElseThrow();
        assertThat(closed.totalMillis()).isEqualTo(200);
        // OPEN 占比 200/400 = 0.5
        assertThat(md.openShare()).isEqualTo(0.5d);
        assertThat(md.openMillis()).isEqualTo(200);
    }

    @Test
    void unorderedInputIsSortedBeforeIntegrating() {
        List<CircuitTransitionJournal.Transition> transitions = List.of(
                t("m", "OPEN", "CLOSED", 300),
                t("m", "CLOSED", "OPEN", 100));
        var report = CircuitStateDurationAnalyzer.analyze(transitions, 500L);
        var md = report.get(0);
        assertThat(md.observedMillis()).isEqualTo(400);
        var open = md.states().stream()
                .filter(s -> s.state().equals("OPEN")).findFirst().orElseThrow();
        assertThat(open.totalMillis()).isEqualTo(200);
    }

    @Test
    void singleTransitionExtendsLastSegmentToNow() {
        var report = CircuitStateDurationAnalyzer.analyze(
                List.of(t("m", "CLOSED", "OPEN", 1000)), 4000L);
        var md = report.get(0);
        assertThat(md.observedMillis()).isEqualTo(3000);
        assertThat(md.states()).hasSize(1);
        assertThat(md.states().get(0).state()).isEqualTo("OPEN");
        assertThat(md.states().get(0).totalMillis()).isEqualTo(3000);
        assertThat(md.openShare()).isEqualTo(1.0d);
    }

    @Test
    void modelsAreGroupedIndependently() {
        List<CircuitTransitionJournal.Transition> transitions = List.of(
                t("a", "CLOSED", "OPEN", 100),
                t("b", "CLOSED", "OPEN", 200));
        var report = CircuitStateDurationAnalyzer.analyze(transitions, 600L);
        assertThat(report).hasSize(2);
        var byA = report.stream().filter(r -> r.model().equals("a")).findFirst().orElseThrow();
        var byB = report.stream().filter(r -> r.model().equals("b")).findFirst().orElseThrow();
        assertThat(byA.observedMillis()).isEqualTo(500);
        assertThat(byB.observedMillis()).isEqualTo(400);
        // 状态列表按总时长降序——「主要泡在哪」第一眼可见
        assertThat(byA.states().get(0).state()).isEqualTo("OPEN");
    }
}
