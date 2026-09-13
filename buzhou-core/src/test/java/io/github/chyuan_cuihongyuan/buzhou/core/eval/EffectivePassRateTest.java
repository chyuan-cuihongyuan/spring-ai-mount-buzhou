package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-685 / spec 933：剪枝 run 有效通过率——prunedCount 精确、effectivePassRate
 * 分母排除 pruned、全 pruned 约定 0.0、无剪枝双口径相等、既有 passRate 零变化。
 */
class EffectivePassRateTest {

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 0);
    }

    private static EvalRunResult run(List<EvalRunItemResult> items) {
        int passed = (int) items.stream().filter(i -> i.status().equals("pass")).count();
        int failed = (int) items.stream().filter(i -> i.status().equals("fail")).count();
        int errored = (int) items.stream().filter(i -> i.status().equals("error")).count();
        return new EvalRunResult("r", "ds", Instant.EPOCH, Instant.EPOCH,
                items.size(), passed, failed, errored, items, null);
    }

    @Test
    void prunedExcludedFromEffectiveDenominator() {
        // 2 pass + 3 pruned：总量口径 2/5=0.4（稀释），有效口径 2/2=1.0
        EvalRunResult run = run(List.of(
                item("a", "pass"), item("b", "pass"),
                item("p1", "pruned"), item("p2", "pruned"), item("p3", "pruned")));
        assertThat(run.prunedCount()).isEqualTo(3);
        assertThat(run.passRate()).isCloseTo(0.4, within(1e-9)); // 既有总量口径不变
        assertThat(run.effectivePassRate()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void allPrunedConventionZero() {
        EvalRunResult run = run(List.of(
                item("p1", "pruned"), item("p2", "pruned")));
        assertThat(run.effectivePassRate()).isZero(); // 全 pruned——有效分母 0 约定 0.0
    }

    @Test
    void noPruneBothRatesEqual() {
        EvalRunResult run = run(List.of(
                item("a", "pass"), item("b", "fail"), item("c", "pass")));
        assertThat(run.effectivePassRate()).isEqualTo(run.passRate());
    }
}
