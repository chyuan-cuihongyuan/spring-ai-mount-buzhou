package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-667 / spec 914：gate 判定环形历史——判定入史（pass/fail 都记）、
 * 新→旧序、16 封顶丢最旧、快照不可变、既有 enforce 判定语义零变化。
 */
class GateHistoryTest {

    private static EvalGate gate(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(dataset, "问题" + i, "ok", null, null);
        }
        return new EvalGate(new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore()));
    }

    private static final class AlwaysPassEvaluator implements Evaluator {
        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            return EvalScore.pass("ok");
        }
    }

    @Test
    void decisionsRecordedNewestFirstAndBothOutcomes() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-hist", 2);

        EvalGate.GateResult failResult = gate.enforce("ds-hist", (a, b, c) -> EvalScore.fail("x"), 0.9);
        EvalGate.GateResult passResult = gate.enforce("ds-hist", (a, b, c) -> EvalScore.pass("ok"), 0.5);

        List<EvalGate.GateDecision> history = gate.history();
        assertThat(history).hasSize(2);
        // 新→旧：最后判定（pass）在前
        assertThat(history.get(0).passed()).isTrue();
        assertThat(history.get(0).runId()).isEqualTo(passResult.runId());
        assertThat(history.get(1).passed()).isFalse();
        assertThat(history.get(1).threshold()).isEqualTo(0.9);
        assertThat(history.get(0).datasetName()).isEqualTo("ds-hist");
        assertThat(history.get(0).at()).isNotNull();
    }

    @Test
    void historyCapacityCappedDroppingOldest() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-cap", 1);
        for (int i = 0; i < EvalGate.HISTORY_CAPACITY + 3; i++) {
            gate.enforce("ds-cap", (a, b, c) -> EvalScore.pass("ok"), 0.5);
        }
        List<EvalGate.GateDecision> history = gate.history();
        assertThat(history).hasSize(EvalGate.HISTORY_CAPACITY);
        // 最旧被丢：首条（最新）runId 不等于最早的 run
        assertThat(history.get(EvalGate.HISTORY_CAPACITY - 1).runId())
                .isNotEqualTo(history.get(0).runId());
    }

    @Test
    void historySnapshotIsImmutable() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-imm", 1);
        gate.enforce("ds-imm", (a, b, c) -> EvalScore.pass("ok"), 0.5);
        List<EvalGate.GateDecision> snapshot = gate.history();
        assertThat(snapshot).isEqualTo(gate.history()); // 同内容
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> snapshot.add(snapshot.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
