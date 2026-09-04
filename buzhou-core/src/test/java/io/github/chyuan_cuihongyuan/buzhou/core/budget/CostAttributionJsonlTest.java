package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 334 / impl-357：归因报表 JSONL 导出（行形状/空账零行/两维/比例）
 * + TokenBudgetHook 打点接线（e2e——同 TokenBudgetHookEndToEndTest 装配手法）。
 */
class CostAttributionJsonlTest {

    @AfterEach
    void resetLedgers() {
        CostAttributionLedger.install(null);
    }

    @Test
    void exportsRowsWithUsdAndShareColumns() throws Exception {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("gpt-x", "tenant-a", 750);
        ledger.record("gpt-x", "tenant-b", 250);

        StringWriter out = new StringWriter();
        long lines = CostAttributionJsonl.export(ledger,
                CostAttributionLedger.Dimension.VIRTUAL_KEY, out);
        assertThat(lines).isEqualTo(2L);
        String[] rows = out.toString().split("\n");
        assertThat(rows[0]).contains("\"dimension\":\"VIRTUAL_KEY\"")
                .contains("\"value\":\"tenant-a\"")
                .contains("\"microUsd\":750")
                .contains("\"usd\":\"0.000750\"")
                .contains("\"shareBp\":7500");
        assertThat(rows[1]).contains("\"shareBp\":2500");
    }

    @Test
    void emptyLedgerExportsZeroLines() throws Exception {
        StringWriter out = new StringWriter();
        long lines = CostAttributionJsonl.export(CostAttributionLedger.create(),
                CostAttributionLedger.Dimension.MODEL, out);
        assertThat(lines).isZero();
        assertThat(out.toString()).isEmpty(); // 空账零行诚实
    }

    @Test
    void bothDimensionsExportIndependently() throws Exception {
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("m1", "k1", 100);
        ledger.record("m2", "k1", 100);
        StringWriter modelOut = new StringWriter();
        StringWriter keyOut = new StringWriter();
        assertThat(CostAttributionJsonl.export(ledger,
                CostAttributionLedger.Dimension.MODEL, modelOut)).isEqualTo(2L);
        assertThat(CostAttributionJsonl.export(ledger,
                CostAttributionLedger.Dimension.VIRTUAL_KEY, keyOut)).isEqualTo(1L);
        assertThat(modelOut.toString()).contains("\"value\":\"m1\"").contains("\"value\":\"m2\"");
        assertThat(keyOut.toString()).contains("\"value\":\"k1\"")
                .contains("\"shareBp\":10000"); // 全额归一
    }

    /** 每次调用附带 usage（100 prompt + 50 completion）的替身模型。 */
    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(100, 50))
                    .build());
        }
    }

    @Test
    void tokenBudgetHookFeedsAttributionLedger() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        TokenBudgetHook hook = new TokenBudgetHook(BuzhouTokenBudgetProperties.defaults(),
                null, null);
        RuntimeConfig config = new RuntimeConfig(List.of(hook), Set.of(), Set.of(), null, List.of());
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        runtime.spawn("app", "agent", "sess-attribution").chat("q1");

        List<CostAttributionLedger.Attribution> byModel =
                CostAttributionLedger.global().rollup(CostAttributionLedger.Dimension.MODEL);
        List<CostAttributionLedger.Attribution> byKey =
                CostAttributionLedger.global().rollup(CostAttributionLedger.Dimension.VIRTUAL_KEY);
        assertThat(byModel).hasSize(1); // 记账点同点双记
        assertThat(byKey).hasSize(1);
        assertThat(byKey.get(0).value()).isEqualTo(CostAttributionLedger.UNATTRIBUTED); // 未接 key → 诚实桶
        assertThat(byKey.get(0).microUsd()).isEqualTo(byModel.get(0).microUsd()); // 两账相等
    }
}
