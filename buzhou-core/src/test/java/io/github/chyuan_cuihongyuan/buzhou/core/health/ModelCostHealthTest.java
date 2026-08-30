package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.ModelCostLedger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 190 §B / T550：模型成本健康面红队——恒 UP 观测；top-8 有界（12 模型
 * 只显 8，distinct 全量）；双口径列（microUsd/usd）一致；空台账诚实零行。
 */
class ModelCostHealthTest {

    @AfterEach
    void cleanup() {
        ModelCostLedger.install(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void boundedTopRowsWithDualDenominations() {
        ModelCostLedger ledger = ModelCostLedger.create();
        for (int i = 0; i < 12; i++) {
            ledger.record("m-" + i, 1_000 * (12 - i)); // used 递减——排行确定
        }
        ModelCostLedger.install(ledger);

        ModelCostHealth health = new ModelCostHealth();
        assertThat(health.mechanism()).isEqualTo("model-cost");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);

        Map<String, Object> details = health.details();
        assertThat(details.get("distinctModels")).isEqualTo(12); // 全量
        assertThat(details.get("totalMicroUsd")).isEqualTo(78_000L);
        assertThat((String) details.get("totalUsd")).isEqualTo("0.078000");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) details.get("topCosts");
        assertThat(rows).hasSize(8); // 有界
        assertThat(rows.get(0).get("model")).isEqualTo("m-0"); // 降序
        assertThat((String) rows.get(0).get("usd")).isEqualTo("0.012000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyLedgerZeroRowsHonest() {
        ModelCostLedger.install(ModelCostLedger.create());
        Map<String, Object> details = new ModelCostHealth().details();
        assertThat(details.get("distinctModels")).isEqualTo(0);
        assertThat(details.get("totalMicroUsd")).isEqualTo(0L);
        assertThat((List<Map<String, Object>>) details.get("topCosts")).isEmpty();
    }
}
