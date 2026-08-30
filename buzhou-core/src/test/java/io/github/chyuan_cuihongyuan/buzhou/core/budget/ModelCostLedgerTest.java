package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 174 §B / T528：模型成本台账红队——累计与稳定排行；零成本也记（跑过
 * 零成本是事实）；封顶折 __overflow__；totalMicroUsd 含 overflow；reset 窗口；
 * 参数 fail-fast。借鉴：WandB/Langfuse cost tracking（账单按模型分组）。
 */
class ModelCostLedgerTest {

    @Test
    void accumulatesAndRanksStably() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 1_000);
        ledger.record("gpt-x", 250);
        ledger.record("claude-y", 800);
        ledger.record("local-z", 0); // 零成本也记

        List<ModelCostLedger.ModelCost> top = ledger.topByCost(3);
        assertThat(top).extracting(ModelCostLedger.ModelCost::model)
                .containsExactly("gpt-x", "claude-y", "local-z");
        assertThat(top.get(0).microUsd()).isEqualTo(1_250L);
        assertThat(ledger.costOf("gpt-x")).isEqualTo(1_250L);
        assertThat(ledger.costOf("never")).isZero();
        assertThat(ledger.totalMicroUsd()).isEqualTo(2_050L);
    }

    @Test
    void overflowFoldsNewModelsButExistingAccumulate() {
        ModelCostLedger ledger = ModelCostLedger.create();
        for (int i = 0; i < ModelCostLedger.MAX_MODELS; i++) {
            ledger.record("m-" + i, 1);
        }
        ledger.record("brand-new", 99); // 折 overflow
        ledger.record("m-0", 5); // 既有继续累计
        assertThat(ledger.costOf(ModelCostLedger.OVERFLOW)).isEqualTo(99L);
        assertThat(ledger.costOf("m-0")).isEqualTo(6L);
        assertThat(ledger.topByCost(1).get(0).model())
                .isEqualTo(ModelCostLedger.OVERFLOW);
        assertThat(ledger.totalMicroUsd())
                .isEqualTo(ModelCostLedger.MAX_MODELS + 99 + 5);
    }

    @Test
    void resetClearsWindowAndValidationFailsFast() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("m", 10);
        ledger.reset();
        assertThat(ledger.distinct()).isZero();
        assertThat(ledger.totalMicroUsd()).isZero();

        assertThatThrownBy(() -> ledger.record(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ledger.record("m", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
