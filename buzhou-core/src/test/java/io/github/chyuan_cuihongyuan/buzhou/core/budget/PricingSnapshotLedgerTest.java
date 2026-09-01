package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 314 / impl-337：价目快照随单回归——快照随单存取 / 调价后行价更新 /
 * JSONL 单价列（有/无快照两态）/ 旧两参兼容。
 */
class PricingSnapshotLedgerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void pricingSnapshotStoredPerModel() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 500L, new ModelCostLedger.PricingSnapshot(
                new BigDecimal("0.15"), new BigDecimal("0.60")));

        ModelCostLedger.PricingSnapshot snapshot = ledger.pricingOf("gpt-x");
        assertThat(snapshot.inputPerMillion()).isEqualByComparingTo("0.15");
        assertThat(snapshot.outputPerMillion()).isEqualByComparingTo("0.60");
    }

    @Test
    void repriceUpdatesRowPricing() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 500L, new ModelCostLedger.PricingSnapshot(
                new BigDecimal("0.15"), new BigDecimal("0.60")));
        // 调价后新记账——行示最近一次记账时单价
        ledger.record("gpt-x", 500L, new ModelCostLedger.PricingSnapshot(
                new BigDecimal("0.30"), new BigDecimal("1.20")));

        assertThat(ledger.costOf("gpt-x")).isEqualTo(1000L);
        assertThat(ledger.pricingOf("gpt-x").inputPerMillion()).isEqualByComparingTo("0.30");
    }

    @Test
    void jsonlCarriesPricingColumnsWhenPresent() throws Exception {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 500L, new ModelCostLedger.PricingSnapshot(
                new BigDecimal("0.15"), new BigDecimal("0.60")));
        ledger.record("free-model", 0L); // 无价目：零成本行不带单价列
        StringWriter out = new StringWriter();

        long lines = ModelCostLedgerJsonl.export(ledger, out);

        assertThat(lines).isEqualTo(2);
        JsonNode[] rows = out.toString().lines().map(line -> {
            try {
                return MAPPER.readTree(line);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toArray(JsonNode[]::new);
        JsonNode priced = "gpt-x".equals(rows[0].get("model").asText()) ? rows[0] : rows[1];
        JsonNode unpriced = priced == rows[0] ? rows[1] : rows[0];
        assertThat(new java.math.BigDecimal(priced.get("inputPerMillion").asText()))
                .isEqualByComparingTo("0.15");
        assertThat(new java.math.BigDecimal(priced.get("outputPerMillion").asText()))
                .isEqualByComparingTo("0.60");
        assertThat(unpriced.has("inputPerMillion"))
                .as("无价目行不带单价列——零成本事实不被伪价污染").isFalse();
    }

    @Test
    void legacyTwoArgRecordKeepsNoPricing() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("legacy-model", 100L); // 旧调用方

        assertThat(ledger.costOf("legacy-model")).isEqualTo(100L);
        assertThat(ledger.pricingOf("legacy-model")).isNull();
    }

    @Test
    void resetClearsPricingToo() {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 1L, new ModelCostLedger.PricingSnapshot(
                BigDecimal.ONE, BigDecimal.TEN));
        ledger.reset();

        assertThat(ledger.pricingOf("gpt-x")).isNull();
    }
}
