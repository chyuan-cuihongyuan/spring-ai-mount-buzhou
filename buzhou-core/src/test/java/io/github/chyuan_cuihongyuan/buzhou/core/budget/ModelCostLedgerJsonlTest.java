package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 188 §B / T548：成本账单导出红队——与 topByCost 同序 + 双口径列
 * （microUsd 精确 / usd 人读 6 位小数）；空表零行诚实；窗口 reset 循环。
 */
class ModelCostLedgerJsonlTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exportsDualDenominationSorted() throws Exception {
        ModelCostLedger ledger = ModelCostLedger.create();
        ledger.record("gpt-x", 1_250);
        ledger.record("claude-y", 800_000);

        StringWriter out = new StringWriter();
        assertThat(ModelCostLedgerJsonl.export(ledger, out)).isEqualTo(2);

        String[] rows = out.toString().split("\n", -1);
        JsonNode first = MAPPER.readTree(rows[0]);
        assertThat(first.get("model").asText()).isEqualTo("claude-y");
        assertThat(first.get("microUsd").asLong()).isEqualTo(800_000L);
        assertThat(first.get("usd").asText()).isEqualTo("0.800000");
        JsonNode second = MAPPER.readTree(rows[1]);
        assertThat(second.get("model").asText()).isEqualTo("gpt-x");
        assertThat(second.get("usd").asText()).isEqualTo("0.001250");
    }

    @Test
    void emptyZeroRowsAndWindowCycle() throws Exception {
        ModelCostLedger ledger = ModelCostLedger.create();
        StringWriter empty = new StringWriter();
        assertThat(ModelCostLedgerJsonl.export(ledger, empty)).isZero();
        assertThat(empty.toString()).isEmpty();

        ledger.record("m", 1);
        ledger.reset();
        StringWriter next = new StringWriter();
        assertThat(ModelCostLedgerJsonl.export(ledger, next)).isZero();
    }
}
