package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 334 / impl-357：成本归因台账回归——双维同笔 / 无 key 诚实桶 /
 * 封顶折溢出 / share 万分比 + 稳定排序 / reset / 零成本也记。
 */
class CostAttributionLedgerTest {

    @Test
    void sameAmountEntersBothDimensions() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("gpt-x", "tenant-a", 1_000);

        List<CostAttributionLedger.Attribution> byModel =
                ledger.rollup(CostAttributionLedger.Dimension.MODEL);
        List<CostAttributionLedger.Attribution> byKey =
                ledger.rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byModel).hasSize(1);
        assertThat(byModel.get(0).value()).isEqualTo("gpt-x");
        assertThat(byKey).hasSize(1);
        assertThat(byKey.get(0).value()).isEqualTo("tenant-a");
        assertThat(ledger.totalMicroUsd(CostAttributionLedger.Dimension.MODEL))
                .isEqualTo(1_000L);
        assertThat(ledger.totalMicroUsd(CostAttributionLedger.Dimension.VIRTUAL_KEY))
                .isEqualTo(1_000L); // 同一笔两账相等
    }

    @Test
    void missingKeyGoesToHonestBucket() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("gpt-x", null, 500);
        List<CostAttributionLedger.Attribution> byKey =
                ledger.rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byKey.get(0).value()).isEqualTo(CostAttributionLedger.UNATTRIBUTED);
        assertThat(byKey.get(0).microUsd()).isEqualTo(500L); // 成本不蒸发
    }

    @Test
    void overCapFoldsIntoOverflow() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        for (int i = 0; i < CostAttributionLedger.MAX_VALUES + 5; i++) {
            ledger.record("m-" + i, null, 1);
        }
        List<CostAttributionLedger.Attribution> byModel =
                ledger.rollup(CostAttributionLedger.Dimension.MODEL);
        assertThat(byModel).hasSize(CostAttributionLedger.MAX_VALUES);
        assertThat(byModel.stream()
                .anyMatch(a -> a.value().equals(CostAttributionLedger.OVERFLOW))).isTrue();
        assertThat(ledger.totalMicroUsd(CostAttributionLedger.Dimension.MODEL))
                .isEqualTo(CostAttributionLedger.MAX_VALUES + 5L); // 总账不丢
    }

    @Test
    void shareBasisPointsAndStableSort() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("m", "k-a", 750);
        ledger.record("m", "k-b", 250);

        List<CostAttributionLedger.Attribution> byKey =
                ledger.rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byKey.get(0).value()).isEqualTo("k-a"); // 降序
        assertThat(byKey.get(0).shareBp()).isEqualTo(7_500L); // 万分比
        assertThat(byKey.get(1).shareBp()).isEqualTo(2_500L);
        assertThat(byKey.get(0).usd()).isEqualTo("0.000750"); // 人读口径
    }

    @Test
    void equalAmountsSortByNameStably() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("m", "k-b", 100);
        ledger.record("m", "k-a", 100);
        List<CostAttributionLedger.Attribution> byKey =
                ledger.rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byKey).extracting(CostAttributionLedger.Attribution::value)
                .containsExactly("k-a", "k-b"); // 同额字典序
    }

    @Test
    void resetOpensNewWindow_zeroCostStillRecorded() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("m", "k", 1_000);
        ledger.reset();
        assertThat(ledger.rollup(CostAttributionLedger.Dimension.MODEL)).isEmpty();
        ledger.record("m", "k", 0); // 零成本也记——归因事实
        List<CostAttributionLedger.Attribution> byKey =
                ledger.rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byKey).hasSize(1);
        assertThat(byKey.get(0).shareBp()).isZero(); // 总零时 share 全零
    }

    @Test
    void blankValueRejected() {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        assertThatThrownBy(() -> new CostAttributionLedger.Attribution(
                CostAttributionLedger.Dimension.MODEL, " ", 1, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
