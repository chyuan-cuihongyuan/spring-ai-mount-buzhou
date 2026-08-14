package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token/成本预算 e2e（spec 16 / T83 / impl-58）：会话累计跨轮正确、hard-stop 拦截下一次调用、
 * 成本价目换算（microUsd 口径）、无 usage 替身零记账、fail-fast。对齐 RunawayEndToEndTest 装配手法。
 */
class TokenBudgetHookEndToEndTest {

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

    /** 跨轮累计：两次 chat 后会话累计 prompt=200 / completion=100 / total=300，事件快照正确。 */
    @Test
    void accumulatesSessionTokensAcrossTurns() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = runtime(model, stores, BuzhouTokenBudgetProperties.defaults());

        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        session.chat("q1");
        session.chat("q2");

        List<SessionEvent> accumulated = events.stream()
                .filter(e -> TokenBudgetHook.EVENT_TOKENS_ACCUMULATED.equals(e.type())).toList();
        assertThat(accumulated).hasSize(2);
        Map<String, Object> last = accumulated.get(1).payload();
        assertThat(last.get("sessionPromptTokens")).isEqualTo(200L);
        assertThat(last.get("sessionCompletionTokens")).isEqualTo(100L);
        assertThat(last.get("sessionTotalTokens")).isEqualTo(300L);
        assertThat(last.get("promptTokens")).isEqualTo(100L); // 本次增量
        session.close();
    }

    /** total-tokens 硬顶：累计触顶后拦截下一次模型调用（模型零调用），block reason 为最终回复。 */
    @Test
    void totalTokenHardStopBlocksNextCall() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 每调用 total=150；上限 250：chat1 后累计 150 < 250 放行 chat2（累计 300），chat3 被拦截
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, 250L, null, null);
        AgentRuntime runtime = runtime(model, stores, props);

        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        session.chat("q1");
        session.chat("q2");
        String blocked = session.chat("q3");

        assertThat(blocked).contains("预算上限");
        assertThat(model.seenPrompts).hasSize(2); // q3 从未触达模型
        assertThat(events).anyMatch(e -> TokenBudgetHook.EVENT_TOKEN_HARD_STOP.equals(e.type())
                && "total-tokens".equals(e.payload().get("reason"))
                && Long.valueOf(250L).equals(e.payload().get("limit")));
        session.close();
    }

    /** 成本硬顶：价目换算 microUsd（token × 每百万价），累计 USD 触顶拦截；事件携带 USD 值。 */
    @Test
    void costHardStopWithPricing() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 价目：input=1.0 / output=2.0 USD 每百万 → 每调用 = 100×1 + 50×2 = 200 microUsd
        // 上限 0.0003 USD = 300 microUsd：chat1 后 200 < 300 放行，chat2 后 400 ≥ 300，chat3 拦截
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, new BigDecimal("0.0003"),
                Map.of("test-model", new BuzhouTokenBudgetProperties.Pricing(
                        new BigDecimal("1.0"), new BigDecimal("2.0"))));
        AgentRuntime runtime = runtime(model, stores, props);

        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        session.chat("q1");
        assertThat(events.stream()
                .filter(e -> TokenBudgetHook.EVENT_TOKENS_ACCUMULATED.equals(e.type()))
                .map(e -> e.payload().get("sessionCostUsd")).toList())
                .containsExactly("0.000200 USD");
        session.chat("q2");
        String blocked = session.chat("q3");

        assertThat(blocked).contains("预算上限");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> TokenBudgetHook.EVENT_COST_HARD_STOP.equals(e.type())
                && "cost-usd".equals(e.payload().get("reason")));
        session.close();
    }

    /** 无 usage 替身零记账：不产生累计事件，硬顶不误伤。 */
    @Test
    void noUsageModelZeroAccounting() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, 1000L, null, null);
        AgentRuntime runtime = runtime(model, stores, props);

        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).isEqualTo("r2");
        assertThat(events).noneMatch(e -> e.type().startsWith("budget."));
        session.close();
    }

    /** fail-fast：成本上限无价目 / 负值上限启动即失败。 */
    @Test
    void configFailFast() {
        assertThatThrownBy(() -> new BuzhouTokenBudgetProperties(
                null, null, null, new BigDecimal("1.0"), null))
                .isInstanceOf(BuzhouConfigurationException.class);
        assertThatThrownBy(() -> new BuzhouTokenBudgetProperties(
                null, 0L, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class);
    }

    // ---- helpers ----

    private static AgentRuntime runtime(ScriptedChatModel model, BuzhouStores stores,
                                        BuzhouTokenBudgetProperties props) {
        TokenBudgetHook hook = new TokenBudgetHook(props, "test-model", stores.observabilityStore());
        RuntimeConfig config = new RuntimeConfig(List.of(hook), Set.of(), Set.of(), null, List.of());
        return Buzhou.runtime(model, stores, config);
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }
}
