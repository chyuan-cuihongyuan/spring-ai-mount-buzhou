package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估 A/A 抖动检测（spec 513 / T777，HELM/工业 A/A test 思想）：同数据集
 * 同版本跑两遍——同项 verdict 红绿翻转 = 抖动项（方向不区分：A/A 无方向，
 * 81 对比器的 REGRESSION/FIX 语义在此不适用）。红绿映射：pass=绿、
 * fail/error=红（321 错误从严——error 不折算 pass）；单侧项（仅一侧存在）
 * = 数据集漂移，不进抖动分母。纯函数不触 store（EvalRunDiff 同型）。
 *
 * <p>诚实边界：两 run 才能判抖动（单 run 无法区分稳定红与抖动）；不自动
 * 重跑 k 次（k 次留宿主循环）。
 */
public final class EvalFlakinessDetector {

    private EvalFlakinessDetector() {
    }

    /** 单项抖动行（两次 run 的各自 status——人审可读）。 */
    public record FlakyItem(String itemId, String statusA, String statusB) {
    }

    /** A/A 抖动报告。 */
    public record FlakinessReport(String runAId, String runBId, int compared,
            List<FlakyItem> flakyItems, List<String> driftItems, double flakyRate) {

        /** 抖动率 = flaky / compared（0 项约定 0——空集是合法状态）。 */
        @Override
        public double flakyRate() {
            return flakyRate;
        }
    }

    /** 逐项对齐分析（datasetName 不同也允许——单侧项自然显形为漂移）。 */
    public static FlakinessReport analyze(EvalRunResult runA, EvalRunResult runB) {
        if (runA == null || runB == null) {
            throw new IllegalArgumentException("两个 run 结果都必须非空");
        }
        Map<String, String> statusA = new LinkedHashMap<>();
        runA.items().forEach(i -> statusA.put(i.itemId(), i.status()));
        Map<String, String> statusB = new LinkedHashMap<>();
        runB.items().forEach(i -> statusB.put(i.itemId(), i.status()));

        List<FlakyItem> flaky = new ArrayList<>();
        List<String> drift = new ArrayList<>();
        int compared = 0;
        for (String id : union(statusA.keySet(), statusB.keySet())) {
            String a = statusA.get(id);
            String b = statusB.get(id);
            if (a == null || b == null) {
                drift.add(id); // 单侧项 = 数据集漂移（不算抖动）
                continue;
            }
            compared++;
            if (isRed(a) != isRed(b)) {
                flaky.add(new FlakyItem(id, a, b));
            }
        }
        double rate = compared == 0 ? 0.0 : (double) flaky.size() / compared;
        return new FlakinessReport(runA.runId(), runB.runId(), compared,
                List.copyOf(flaky), List.copyOf(drift), rate);
    }

    /** 红 = fail | error（321 错误从严——error 不折算 pass）。 */
    private static boolean isRed(String status) {
        return EvalRunItemResult.STATUS_FAIL.equals(status)
                || EvalRunItemResult.STATUS_ERROR.equals(status);
    }

    private static java.util.Set<String> union(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(a);
        all.addAll(b);
        return all;
    }
}
