package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 两次评估 run 的逐项对比（spec 81 §A / T315，LangSmith run compare / Promptfoo
 * trend 借鉴）：同 itemId 对齐状态迁移——REGRESSION（绿变红）/ FIX（红变绿）/
 * STABLE_PASS / STABLE_FAIL（红态内部 fail↔error 不细分——都是红）；单侧项
 * （BASE_ONLY/HEAD_ONLY——两 run 间数据集漂移）单独计数不进四态。纯函数：输入
 * 两个 {@link EvalRunResult}（可从 {@link EvalQueryService#run} 回读），不触 store。
 */
public final class EvalRunDiff {

    private EvalRunDiff() {
    }

    /** 项级状态迁移分类。 */
    public enum Change {
        REGRESSION, FIX, STABLE_PASS, STABLE_FAIL, BASE_ONLY, HEAD_ONLY
    }

    /** 单项对比行（before/after 为状态；detail 取各自首行——摘要可读）。 */
    public record ItemDiff(String itemId, String before, String after, Change change) {
    }

    /** 对比汇总（四态计数 + 单侧计数 + 净变化 + 明细行按 itemId 稳定序）。 */
    public record DiffResult(String baseRunId, String headRunId, int regressions, int fixes,
                             int stablePass, int stableFail, int baseOnly, int headOnly,
                             List<ItemDiff> items) {

        /** 净变化 = fixes - regressions（正 = 变好；CI 门/趋势判断用）。 */
        public int netDelta() {
            return fixes - regressions;
        }
    }

    /** 逐项对比（base 与 head 的 datasetName 不一致也允许——单侧项自然显形）。 */
    public static DiffResult diff(EvalRunResult base, EvalRunResult head) {
        Map<String, String> baseStatus = new LinkedHashMap<>();
        base.items().forEach(i -> baseStatus.put(i.itemId(), i.status()));
        Map<String, String> headStatus = new LinkedHashMap<>();
        head.items().forEach(i -> headStatus.put(i.itemId(), i.status()));
        Set<String> allIds = new LinkedHashSet<>();
        allIds.addAll(baseStatus.keySet());
        allIds.addAll(headStatus.keySet());

        int regressions = 0;
        int fixes = 0;
        int stablePass = 0;
        int stableFail = 0;
        int baseOnly = 0;
        int headOnly = 0;
        List<ItemDiff> items = new ArrayList<>();
        for (String id : allIds) {
            String before = baseStatus.get(id);
            String after = headStatus.get(id);
            Change change;
            if (before == null) {
                change = Change.HEAD_ONLY;
                headOnly++;
            } else if (after == null) {
                change = Change.BASE_ONLY;
                baseOnly++;
            } else {
                boolean beforeGreen = EvalRunItemResult.STATUS_PASS.equals(before);
                boolean afterGreen = EvalRunItemResult.STATUS_PASS.equals(after);
                if (beforeGreen && !afterGreen) {
                    change = Change.REGRESSION;
                    regressions++;
                } else if (!beforeGreen && afterGreen) {
                    change = Change.FIX;
                    fixes++;
                } else if (beforeGreen) {
                    change = Change.STABLE_PASS;
                    stablePass++;
                } else {
                    change = Change.STABLE_FAIL;
                    stableFail++;
                }
            }
            items.add(new ItemDiff(id, before, after, change));
        }
        return new DiffResult(base.runId(), head.runId(), regressions, fixes, stablePass,
                stableFail, baseOnly, headOnly, List.copyOf(items));
    }

    // ---- 测试/宿主便捷构造（纯函数输入面） ----

    /** 便捷构造：给定项状态序列（itemId:status），时间戳取 now。 */
    public static EvalRunResult runOf(String runId, String datasetName, Map<String, String> itemStatuses) {
        Instant now = Instant.now();
        List<EvalRunItemResult> items = new ArrayList<>();
        itemStatuses.forEach((id, status) -> items.add(
                new EvalRunItemResult(id, status, null, null, 0L)));
        int passed = (int) items.stream().filter(i -> EvalRunItemResult.STATUS_PASS.equals(i.status())).count();
        int failed = (int) items.stream().filter(i -> EvalRunItemResult.STATUS_FAIL.equals(i.status())).count();
        int errored = (int) items.stream().filter(i -> EvalRunItemResult.STATUS_ERROR.equals(i.status())).count();
        return new EvalRunResult(runId, datasetName, now, now, items.size(), passed, failed,
                errored, items);
    }
}
