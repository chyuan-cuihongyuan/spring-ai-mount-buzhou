package io.github.chyuan_cuihongyuan.buzhou.resilience.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * agent 级成本归集红队（spec 65 §B / T282）：双「实例」并发累计精确（跨实例原子）；
 * agent 隔离；定价换算；无价目 microUsd=0 tokens 仍归集；查询解析与净化。
 */
class AgentCostLedgerHookTest {

    private static BuzhouTokenBudgetProperties pricing(String model, double in, double out) {
        return new BuzhouTokenBudgetProperties(null, null, null, null,
                Map.of(model, new BuzhouTokenBudgetProperties.Pricing(
                        BigDecimal.valueOf(in), BigDecimal.valueOf(out))));
    }

    private static ChatClientResponse usageResponse(String model, int prompt, int completion) {
        ChatResponse chat = new ChatResponse(List.of(new Generation(new AssistantMessage("r"))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(prompt, completion))
                        .model(model).build());
        return new ChatClientResponse(chat, Map.of());
    }

    private static final class StubCtx implements ModelCallContext {
        private final HookEnvironment env;
        private final ChatClientResponse response;

        StubCtx(HookEnvironment env, ChatClientResponse response) {
            this.env = env;
            this.response = response;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return 1; }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(SessionEvent event) { }
        @Override public ChatClientRequest request() { return null; }
        @Override public ChatClientResponse response() { return response; }
        @Override public Throwable error() { return null; }
        @Override public void replaceRequest(ChatClientRequest newRequest) { }
        @Override public void replaceResponse(ChatClientResponse newResponse) { }
    }

    @Test
    void accumulatesAcrossSessionsAndInstancesAtomically() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        AgentCostLedgerHook ledger = new AgentCostLedgerHook(pricing("m", 1.0, 2.0), "m", store);
        int instances = 2;
        int threads = 8;
        int calls = 10;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threads)) {
            for (int i = 0; i < instances; i++) {
                HookEnvironment env = new HookEnvironment("sess-" + i, "support-agent", store);
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        for (int n = 0; n < calls; n++) {
                            ledger.afterModel(new StubCtx(env, usageResponse("m", 100, 50)));
                        }
                        return null;
                    }));
                }
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        }
        int totalCalls = instances * threads * calls;
        List<AgentCostLedgerHook.AgentCostRow> rows = AgentCostLedgerHook.query(store);
        assertThat(rows).hasSize(1);
        AgentCostLedgerHook.AgentCostRow row = rows.getFirst();
        assertThat(row.agentName()).isEqualTo("support-agent");
        assertThat(row.promptTokens()).isEqualTo(totalCalls * 100L);
        assertThat(row.completionTokens()).isEqualTo(totalCalls * 50L);
        // microUsd = prompt×1.0 + completion×2.0（百万分之口径：tokens×price）
        assertThat(row.microUsd()).isEqualTo(totalCalls * (100L * 1 + 50L * 2));
    }

    @Test
    void agentsAreIsolatedAndUnpricedModelStillCountsTokens() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        AgentCostLedgerHook ledger = new AgentCostLedgerHook(pricing("m", 1.0, 2.0), "m", store);

        HookEnvironment a = new HookEnvironment("s1", "agent-a", store);
        HookEnvironment b = new HookEnvironment("s2", "agent-b", store);
        ledger.afterModel(new StubCtx(a, usageResponse("m", 100, 100)));
        ledger.afterModel(new StubCtx(b, usageResponse("unknown-model", 70, 30))); // 无价目

        List<AgentCostLedgerHook.AgentCostRow> rows = AgentCostLedgerHook.query(store);
        assertThat(rows).extracting(AgentCostLedgerHook.AgentCostRow::agentName)
                .containsExactlyInAnyOrder("agent-a", "agent-b"); // 隔离
        AgentCostLedgerHook.AgentCostRow rowB = rows.stream()
                .filter(r -> r.agentName().equals("agent-b")).findFirst().orElseThrow();
        assertThat(rowB.promptTokens()).isEqualTo(70); // tokens 仍归集
        assertThat(rowB.microUsd()).isZero(); // 无价目 = 0
    }

    @Test
    void agentNameSanitizedIntoKey() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        AgentCostLedgerHook ledger = new AgentCostLedgerHook(pricing("m", 1.0, 1.0), "m", store);
        HookEnvironment env = new HookEnvironment("s1", "ops/agent:生产", store);
        ledger.afterModel(new StubCtx(env, usageResponse("m", 10, 10)));
        assertThat(store.getAll(AgentCostLedgerHook.LEDGER_SESSION).keySet())
                .allMatch(k -> k.matches("agent\\.[A-Za-z0-9._-]+\\..*")); // 键净化
        assertThat(AgentCostLedgerHook.query(store)).hasSize(1);
    }

    @Test
    void emptyLedgerQueriesEmpty() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        assertThat(AgentCostLedgerHook.query(store)).isEmpty();
    }
}
