package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-692 / spec 943：k 次防抖门——k 次全过才过、任一失败早停、历史条数、
 * k 越界 fail-fast、单次门语义零变化。
 */
class StableGateTest {

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

    @Test
    void allPassRequiredWithinK() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-stable", 2);
        AtomicInteger seq = new AtomicInteger();
        // 首次 pass、之后 fail——k=2 应 fail（早停后不再跑第 3 次）
        EvalGate.GateResult result = gate.enforceStable("ds-stable", (a, b, c) -> {
            int n = seq.getAndIncrement() / 2; // 每 2 次评估切换（每 run 2 项）
            return n == 0 ? EvalScore.pass("ok") : EvalScore.fail("x");
        }, 0.5, 2);
        assertThat(result.passed()).isFalse(); // 第 2 次 run 失败
    }

    @Test
    void allPassAcrossKRunsPasses() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-stable-pass", 2);
        EvalGate.GateResult result = gate.enforceStable("ds-stable-pass",
                (a, b, c) -> EvalScore.pass("ok"), 0.5, 3);
        assertThat(result.passed()).isTrue();
        assertThat(gate.history()).hasSize(3); // 3 次判定全留史
    }

    @Test
    void kExceedingHistoryCapacityFailsFast() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-cap", 1);
        assertThatThrownBy(() -> gate.enforceStable("ds-cap",
                (a, b, c) -> EvalScore.pass("ok"), 0.5, EvalGate.HISTORY_CAPACITY + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kOneEqualsPlainEnforce() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-k1", 2);
        EvalGate.GateResult result = gate.enforceStable("ds-k1",
                (a, b, c) -> EvalScore.pass("ok"), 0.5, 1);
        assertThat(result.passed()).isTrue();
        assertThat(gate.history()).hasSize(1); // k=1 = 单次判定等价
    }
}
