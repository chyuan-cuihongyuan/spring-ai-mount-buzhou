package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-669 / spec 916：pruned×稳定性×gate 联动——pruned（未执行）不得被红绿
 * 映射误判为绿（假稳定探测）、有效样本不足的项不 compared、gate 历史记录
 * 剪枝 run 正常。
 */
class PrunedStabilityE2ETest {

    private static EvalRunResult run(String runId, EvalRunItemResult... items) {
        List<EvalRunItemResult> list = List.of(items);
        long passed = list.stream().filter(i -> i.status().equals("pass")).count();
        long failed = list.stream().filter(i -> i.status().equals("fail")).count();
        long errored = list.stream().filter(i -> i.status().equals("error")).count();
        long pruned = list.size() - passed - failed - errored;
        return new EvalRunResult(runId, "ds", Instant.EPOCH, Instant.EPOCH,
                items.length, (int) passed, (int) failed, (int) errored, list, null);
    }

    private static EvalRunItemResult item(String id, String status) {
        return new EvalRunItemResult(id, status, "", "", 0);
    }

    @Test
    void prunedDoesNotManufactureFalseStability() {
        // 项 x：r1 真 fail、r2/r3 pruned——若无修复，pruned 被当绿 → 两绿一红仍抖动；
        // 但更糟的场景：全 pruned + 真 fail 在 3 run 中「2 pruned 同色」假象。
        // 修复后语义：pruned = 无有效样本 → x 仅 1 个有效样本 → 不 compared。
        EvalFlakinessDetector.KStabilityReport report = EvalFlakinessDetector.analyzeK(List.of(
                run("r1", item("x", "fail")),
                run("r2", item("x", "pruned")),
                run("r3", item("x", "pruned"))));

        assertThat(report.compared()).isZero(); // 有效样本不足——不判定
        assertThat(report.flakyItems()).isZero();
        assertThat(report.verdicts()).isEmpty(); // 无有效样本 = 无 verdict
    }

    @Test
    void partialPrunedJudgedByEffectiveSamplesOnly() {
        // 项 y：r1 pass、r2 fail、r3/r4 pruned——按 2 个有效样本判定翻转（真抖动显形）
        EvalFlakinessDetector.KStabilityReport report = EvalFlakinessDetector.analyzeK(List.of(
                run("r1", item("y", "pass")),
                run("r2", item("y", "fail")),
                run("r3", item("y", "pruned")),
                run("r4", item("y", "pruned"))));

        assertThat(report.compared()).isEqualTo(1);
        assertThat(report.flakyItems()).isEqualTo(1);
        assertThat(report.verdicts().get(0).statuses())
                .containsExactly("pass", "fail", null, null);
    }

    @Test
    void twoRunVersionAlsoExcludesPruned() {
        // 两 run 版同语义加固：含 pruned 的项不 compared
        EvalRunResult a = run("r1", item("z", "fail"), item("ok", "pass"));
        EvalRunResult b = run("r2", item("z", "pruned"), item("ok", "pass"));
        EvalFlakinessDetector.FlakinessReport report = EvalFlakinessDetector.analyze(a, b);
        assertThat(report.compared()).isEqualTo(1); // 仅 ok 入分母；z 被 pruned 排除
        assertThat(report.flakyItems()).isEmpty();
    }

    @Test
    void gateHistoryRecordsPrunedRunNormally() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ds-pruned-gate", null);
        ds.addItem("ds-pruned-gate", "q1", "ok", null, null);
        ds.addItem("ds-pruned-gate", "q2", "ok", null, null);
        ds.addItem("ds-pruned-gate", "q3", "ok", null, null);
        EvalRunner runner = new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore());
        runner.setPrunePolicy(new EvalPrunePolicy(1, 0.99));
        EvalGate gate = new EvalGate(runner);

        EvalGate.GateResult result = gate.enforce("ds-pruned-gate", (actual, expected, item) ->
                EvalScore.fail("no"), 0.5);

        assertThat(result.passed()).isFalse();
        assertThat(gate.history()).hasSize(1);
        assertThat(gate.history().get(0).passed()).isFalse();
        // 断言里的 enforce run 结果含 pruned 项（1 观察窗 fail + 后续 pruned）
        EvalRunResult prunedRun = runner.run("ds-pruned-gate", (a, b, c) -> EvalScore.fail("no"), 1);
        assertThat(prunedRun.items()).anySatisfy(i ->
                assertThat(i.status()).isEqualTo(EvalRunItemResult.STATUS_PRUNED));
    }
}
