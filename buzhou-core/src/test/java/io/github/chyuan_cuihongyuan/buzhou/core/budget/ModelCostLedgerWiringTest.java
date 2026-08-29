package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 176 §B / T531：成本台账接线 e2e——带价目时 afterModel 按模型入账
 * （跨会话累计同一模型）；无价目 = 零成本也记（在册零值行）；窗口 reset 后
 * 重新起账。
 */
class ModelCostLedgerWiringTest {

    @AfterEach
    void cleanup() {
        ModelCostLedger.install(null);
    }

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
    void accumulatesPerModelAcrossSessionsWithPricing() {
        ModelCostLedger.install(ModelCostLedger.create());
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, null, Map.of("test-model",
                new BuzhouTokenBudgetProperties.Pricing(
                        new BigDecimal("3"), new BigDecimal("15"))));
        TokenBudgetHook hook = new TokenBudgetHook(props, "test-model",
                stores.observabilityStore());
        var runtime = Buzhou.runtime(model, stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        List.of(hook), Set.of(), Set.of(), null, List.of()));

        // 两次调用（两个会话）：每次 100×3 + 50×15 = 1050 microUsd
        var s1 = runtime.spawn("app", "agent", "s1");
        s1.chat("q1");
        s1.close();
        var s2 = runtime.spawn("app", "agent", "s2");
        s2.chat("q2");
        s2.close();

        assertThat(ModelCostLedger.global().costOf("test-model")).isEqualTo(2_100L);
        assertThat(ModelCostLedger.global().topByCost(1).get(0).microUsd())
                .isEqualTo(2_100L);

        ModelCostLedger.global().reset(); // 窗口切换
        assertThat(ModelCostLedger.global().costOf("test-model")).isZero();
    }

    @Test
    void noPricingRecordsZeroCostHonest() {
        ModelCostLedger.install(ModelCostLedger.create());
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        TokenBudgetHook hook = new TokenBudgetHook(BuzhouTokenBudgetProperties.defaults(),
                "free-model", stores.observabilityStore());
        var runtime = Buzhou.runtime(model, stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        List.of(hook), Set.of(), Set.of(), null, List.of()));
        var s3 = runtime.spawn("app", "agent", "s3");
        s3.chat("q");
        s3.close();

        // 无价目 = 零成本，但在册（「跑过零成本」是账单事实）
        assertThat(ModelCostLedger.global().costOf("free-model")).isZero();
        assertThat(ModelCostLedger.global().distinct()).isEqualTo(1);
    }
}
