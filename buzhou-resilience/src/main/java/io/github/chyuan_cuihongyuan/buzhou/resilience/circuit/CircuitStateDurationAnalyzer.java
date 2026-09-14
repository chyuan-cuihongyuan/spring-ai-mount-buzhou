package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 断路器状态时长分析器（spec 1408 / T2117 / impl 1061）——Resilience4j
 * circuit breaker metrics（state duration 语义）思想：{@link CircuitTransitionJournal}
 * 只记变迁时刻，「这台模型的断路器这一小时有多久泡在 OPEN」须把变迁序列
 * 积分成<b>逐状态停留时长</b>才可读。纯函数：吃 journal 的 Transition 列表，
 * 按模型分组积段（[t_i → t_{i+1}) 归 transition_i 的 to 状态；末段延伸至 now），
 * 出每状态总时长/段数/最长段 + 观测窗与 OPEN 占比（crash-loop 显形）。
 *
 * <p>只读分析零接线：journal/断路器语义逐位不变； Clock 以 nowEpochMs 入参
 * 注入（确定性测试）。采样型口径如实入档：journal 有界环丢弃的旧变迁不参与
 * 积分——报告以现存最早变迁为窗口起点。
 */
public final class CircuitStateDurationAnalyzer {

    private CircuitStateDurationAnalyzer() {
    }

    /**
     * @param state       断路器状态名（journal 的 to/from 原文，如 CLOSED/OPEN/HALF_OPEN）
     * @param totalMillis 该状态累计停留（观测窗内）
     * @param segments    停留段数（每次进入该状态算一段）
     * @param maxMillis   单段最长停留
     */
    public record StateDuration(String state, long totalMillis, int segments, long maxMillis) {
    }

    /**
     * @param model          模型名
     * @param states         逐状态时长（总时长降序——「主要泡在哪」第一眼可见）
     * @param observedMillis 观测窗长（now − 现存最早变迁时刻）
     * @param openMillis     OPEN 态累计停留（0 = 未进过 OPEN）
     */
    public record ModelDurations(String model, List<StateDuration> states,
                                 long observedMillis, long openMillis) {

        /** OPEN 占比（观测窗内；窗长 0 哨兵 -1）。 */
        public double openShare() {
            return observedMillis == 0 ? -1d : (double) openMillis / observedMillis;
        }
    }

    /** 分析入口：journal.snapshot().recent() 原样喂入即可（无序容忍，内部按时刻升序）。 */
    public static List<ModelDurations> analyze(List<CircuitTransitionJournal.Transition> transitions,
                                               long nowEpochMs) {
        if (transitions == null || transitions.isEmpty()) {
            return List.of();
        }
        List<CircuitTransitionJournal.Transition> sorted =
                new ArrayList<>(transitions);
        sorted.sort(Comparator.comparingLong(CircuitTransitionJournal.Transition::atEpochMs));

        Map<String, List<CircuitTransitionJournal.Transition>> byModel = new LinkedHashMap<>();
        for (CircuitTransitionJournal.Transition t : sorted) {
            byModel.computeIfAbsent(t.model(), k -> new ArrayList<>()).add(t);
        }

        List<ModelDurations> result = new ArrayList<>();
        for (Map.Entry<String, List<CircuitTransitionJournal.Transition>> entry
                : byModel.entrySet()) {
            result.add(analyzeModel(entry.getKey(), entry.getValue(), nowEpochMs));
        }
        return List.copyOf(result);
    }

    private static ModelDurations analyzeModel(String model,
                                               List<CircuitTransitionJournal.Transition> modelTransitions,
                                               long nowEpochMs) {
        long windowStart = modelTransitions.get(0).atEpochMs();
        Map<String, long[]> buckets = new LinkedHashMap<>(); // state → [total, segments, max]
        long openMillis = 0;

        for (int i = 0; i < modelTransitions.size(); i++) {
            CircuitTransitionJournal.Transition t = modelTransitions.get(i);
            long segmentEnd = (i + 1 < modelTransitions.size())
                    ? modelTransitions.get(i + 1).atEpochMs() : Math.max(nowEpochMs, t.atEpochMs());
            long duration = Math.max(0, segmentEnd - t.atEpochMs());
            long[] bucket = buckets.computeIfAbsent(t.to(),
                    k -> new long[]{0, 0, 0});
            bucket[0] += duration;
            bucket[1]++;
            bucket[2] = Math.max(bucket[2], duration);
            if ("OPEN".equals(t.to())) {
                openMillis += duration;
            }
        }

        List<StateDuration> states = new ArrayList<>();
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            states.add(new StateDuration(e.getKey(), e.getValue()[0],
                    (int) e.getValue()[1], e.getValue()[2]));
        }
        states.sort(Comparator.comparingLong(StateDuration::totalMillis).reversed());
        return new ModelDurations(model, List.copyOf(states),
                Math.max(0, nowEpochMs - windowStart), openMillis);
    }
}
