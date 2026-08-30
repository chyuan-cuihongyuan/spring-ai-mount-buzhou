package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 81 §B / T316：run 对比红队——四态迁移分类正确（含 fail↔error 同为红态）；
 * 单侧项（数据集漂移）单独计数；netDelta；项序稳定；便捷构造计数一致。
 * 借鉴：LangSmith run compare / Promptfoo trend diff。
 */
class EvalRunDiffTest {

    private static EvalRunResult run(String runId, String... itemIdColonStatus) {
        Map<String, String> statuses = new LinkedHashMap<>();
        for (String pair : itemIdColonStatus) {
            int c = pair.indexOf(':');
            statuses.put(pair.substring(0, c), pair.substring(c + 1));
        }
        return EvalRunDiff.runOf(runId, "ds", statuses);
    }

    @Test
    void classifiesFourWayTransitions() {
        EvalRunDiff.DiffResult diff = EvalRunDiff.diff(
                run("base", "i1:pass", "i2:pass", "i3:fail", "i4:fail", "i5:error", "i6:pass"),
                run("head", "i1:pass", "i2:fail", "i3:pass", "i4:error", "i5:pass", "i6:pass"));

        assertThat(diff.regressions()).isEqualTo(1); // i2 绿→红
        assertThat(diff.fixes()).isEqualTo(2); // i3 红→绿；i5 error→pass 也是 FIX
        assertThat(diff.stablePass()).isEqualTo(2); // i1, i6
        assertThat(diff.stableFail()).isEqualTo(1); // i4 fail→error：红态内部不细分
        assertThat(diff.netDelta()).isEqualTo(1); // 2 fix - 1 regression
        assertThat(diff.items()).extracting(EvalRunDiff.ItemDiff::change)
                .containsExactly(EvalRunDiff.Change.STABLE_PASS, EvalRunDiff.Change.REGRESSION,
                        EvalRunDiff.Change.FIX, EvalRunDiff.Change.STABLE_FAIL,
                        EvalRunDiff.Change.FIX, EvalRunDiff.Change.STABLE_PASS);
    }

    @Test
    void datasetDriftSurfacesAsSingleSidedItems() {
        EvalRunDiff.DiffResult diff = EvalRunDiff.diff(
                run("base", "kept:pass", "removed:fail"),
                run("head", "kept:pass", "added1:pass", "added2:error"));

        assertThat(diff.baseOnly()).isEqualTo(1); // removed
        assertThat(diff.headOnly()).isEqualTo(2); // added1/added2
        assertThat(diff.stablePass()).isEqualTo(1);
        assertThat(diff.regressions()).isZero();
        assertThat(diff.fixes()).isZero();
        assertThat(diff.items()).extracting(EvalRunDiff.ItemDiff::change)
                .contains(EvalRunDiff.Change.BASE_ONLY, EvalRunDiff.Change.HEAD_ONLY,
                        EvalRunDiff.Change.HEAD_ONLY);
    }

    @Test
    void convenienceConstructorCountsMatchStatuses() {
        EvalRunResult r = run("r", "a:pass", "b:fail", "c:error");

        assertThat(r.total()).isEqualTo(3);
        assertThat(r.passed()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(1);
        assertThat(r.errored()).isEqualTo(1);
        assertThat(r.passRate()).isEqualTo(1.0 / 3);
    }

    @Test
    void itemOrderStableByIdAcrossDiff() {
        EvalRunDiff.DiffResult diff = EvalRunDiff.diff(
                run("base", "b1:pass", "a1:pass"),
                run("head", "a1:pass", "b1:pass", "c1:fail"));

        // 项序 = base 序优先、head 新增随后（LinkedHashSet 语义——确定不漂移）
        assertThat(diff.items()).extracting(EvalRunDiff.ItemDiff::itemId)
                .containsExactly("b1", "a1", "c1");
    }
}
