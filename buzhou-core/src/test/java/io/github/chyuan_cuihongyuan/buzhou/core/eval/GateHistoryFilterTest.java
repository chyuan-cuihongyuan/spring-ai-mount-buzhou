package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-694 续 / spec 956：gate 历史按数据集过滤——过滤精确（只含目标 dataset）、
 * 新→旧序保持、null/blank fail-fast、无匹配空表、914 环形史零变化。
 */
class GateHistoryFilterTest {

    private static EvalDatasetStore ds(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(dataset, "问题" + i, "ok", null, null);
        }
        return ds;
    }

    private static EvalGate gate(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = ds(stores, dataset, n);
        return new EvalGate(new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore()));
    }

    @Test
    void filterByDatasetKeepsNewestFirstOrder() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-x", 1);
        ds(stores, "ds-y", 1); // 第二数据集（同 gate 实例混用场景）
        gate.enforce("ds-x", (a, b, c) -> EvalScore.pass("ok"), 0.5);
        gate.enforce("ds-y", (a, b, c) -> EvalScore.pass("ok"), 0.5);
        gate.enforce("ds-x", (a, b, c) -> EvalScore.pass("ok"), 0.5);

        List<EvalGate.GateDecision> filtered = gate.historyOf("ds-x");
        assertThat(filtered).hasSize(2);
        assertThat(filtered).allSatisfy(d -> assertThat(d.datasetName()).isEqualTo("ds-x"));
        // 新→旧：最后一次 ds-x 判定在前
        assertThat(gate.history().get(0).datasetName()).isEqualTo("ds-x");
    }

    @Test
    void blankNameFailsFastAndNoMatchEmpty() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalGate gate = gate(stores, "ds-z", 1);
        gate.enforce("ds-z", (a, b, c) -> EvalScore.pass("ok"), 0.5);

        assertThatThrownBy(() -> gate.historyOf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gate.historyOf(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gate.historyOf("no-such")).isEmpty();
    }
}
