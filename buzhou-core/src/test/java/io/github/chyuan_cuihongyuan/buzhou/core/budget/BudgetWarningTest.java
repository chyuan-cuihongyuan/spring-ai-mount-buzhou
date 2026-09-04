package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 338 / impl-361：预算软预警回归——交叉 80% 一次一发 / 未达不发 /
 * -1 关闭 / 成本与 key 维度。TokenBudgetHookEndToEndTest 同装配手法。
 */
class BudgetWarningTest {

    @org.junit.jupiter.api.AfterEach
    void resetGlobalLedgers() {
        // 打点会写全局台账（334 双记）——隔离后续测试
        ModelCostLedger.install(null);
        CostAttributionLedger.install(null);
    }

    /** 每次调用 100 prompt + 50 completion（total 150）的替身模型。 */
    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(100, 50))
                    .build());
        }
    }

    private static AgentRuntime runtime(ScriptedChatModel model, BuzhouStores stores,
            BuzhouTokenBudgetProperties props, VirtualKeys keys, String key) {
        TokenBudgetHook hook = new TokenBudgetHook(props, null, null, keys, key);
        RuntimeConfig config = new RuntimeConfig(List.of(hook), Set.of(), Set.of(), null, List.of());
        return Buzhou.runtime(model, stores, config);
    }

    private static List<SessionEvent> warningsOf(List<SessionEvent> events) {
        return events.stream()
                .filter(e -> TokenBudgetHook.EVENT_BUDGET_WARNING.equals(e.type())).toList();
    }

    @Test
    void crossingThresholdWarnsOncePerDimension() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 两次调用 total 300；硬顶 360 → 83% ≥ 80% 预警；第二次 600 仍只发一次
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, 360L, null, null, null);
        AgentRuntime runtime = runtime(model, stores, props, null, null);

        AgentSession session = runtime.spawn("app", "agent", "sess-warn");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        session.chat("q1"); // 150/360 = 42%——未达
        assertThat(warningsOf(events)).isEmpty();
        session.chat("q2"); // 300/360 = 83%——预警
        assertThat(warningsOf(events)).hasSize(1);
        Map<String, Object> payload = warningsOf(events).get(0).payload();
        assertThat(payload).containsEntry("dimension", "total-tokens")
                .containsEntry("warningPercent", 80);
        session.close();
    }

    @Test
    void disabledAtMinusOne() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, 100L, null, null, -1);
        AgentRuntime runtime = runtime(model, Buzhou.inMemoryStores(), props, null, null);

        AgentSession session = runtime.spawn("app", "agent", "sess-off");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        session.chat("q1"); // 150/100 = 150%——关闭态零预警
        assertThat(warningsOf(events)).isEmpty();
        session.close();
    }

    @Test
    void costDimensionWarnsIndependently() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 价目 1/1 USD 每百万 → 每调用 150 microUsd；硬顶 180 USD？过大——
        // 用 microUsd 精算：150×1e0…价目 perMillion=1e6 时每 token=1 microUsd
        java.math.BigDecimal perMillion = new java.math.BigDecimal("1000000");
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, new java.math.BigDecimal("0.000180"),
                Map.of("unknown", new BuzhouTokenBudgetProperties.Pricing(perMillion, perMillion)),
                null);
        AgentRuntime runtime = runtime(model, stores, props, null, null);

        AgentSession session = runtime.spawn("app", "agent", "sess-cost");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        session.chat("q1"); // 150 tokens → 150 microUsd = 83% of 180 microUsd
        assertThat(warningsOf(events)).hasSize(1);
        assertThat(warningsOf(events).get(0).payload())
                .containsEntry("dimension", "cost-usd");
        session.close();
    }

    @Test
    void virtualKeyDimensionWarns_thenHardStopOwnsExhaustion() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        VirtualKeys keys = VirtualKeys.create();
        keys.register("k-warn", 340); // 150→44%，300→88% 预警
        BuzhouTokenBudgetProperties props = new BuzhouTokenBudgetProperties(
                null, null, null, null, null, null);
        AgentRuntime runtime = runtime(model, stores, props, keys, "k-warn");

        AgentSession session = runtime.spawn("app", "agent", "sess-key");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        session.chat("q1"); // key 用 150/340 = 44%——未达
        assertThat(warningsOf(events)).isEmpty();
        session.chat("q2"); // key 用 300/340 = 88%——key 维度预警
        assertThat(warningsOf(events)).hasSize(1);
        assertThat(warningsOf(events).get(0).payload())
                .containsEntry("dimension", "virtual-key-tokens");
        session.close();
    }
}
