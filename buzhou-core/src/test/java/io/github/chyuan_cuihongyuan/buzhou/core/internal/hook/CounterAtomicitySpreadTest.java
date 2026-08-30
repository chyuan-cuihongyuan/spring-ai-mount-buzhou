package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.TokenBudgetHook;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters;
import io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 计数写路径原子化推广红队（spec 62 §B / T276）：runaway 会话级工具计数与 budget
 * microUsd/token 累计在多「实例」（多 HookEnvironment 共享 store）并发下终值精确
 * ——AtomicStateCounters 进度检测 CAS 钉住不丢更新。
 */
class CounterAtomicitySpreadTest {

    /** runaway 会话计数：4 实例 × 8 线程 × 25 次 beforeTool = 800，终值精确。 */
    @Test
    void runawaySessionToolCountLosesNoUpdates() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(null,
                new BuzhouRunawayProperties.PerTurn(100_000, 100_000, Duration.ofMinutes(30)),
                new BuzhouRunawayProperties.PerSession(1_000_000, 1_000_000),
                null, null, null, null);
        int instances = 4;
        int threads = 8;
        int calls = 25;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threads)) {
            for (int i = 0; i < instances; i++) {
                HookEnvironment env = new HookEnvironment("sess-rw", "agent", store);
                RunawayHook hook = new RunawayHook(props, new RunawayCounters());
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        for (int n = 0; n < calls; n++) {
                            hook.beforeTool(new StubToolCallContext(env));
                        }
                        return null;
                    }));
                }
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
        String raw = store.get("sess-rw", "runaway.session.tool-calls")
                .map(io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry::value).orElse(null);
        assertThat(raw).isEqualTo(String.valueOf(instances * threads * calls));
    }

    /** budget 累计：双实例各 10 线程 × usage(50,50) → prompt/completion 各 1000，终值精确。 */
    @Test
    void budgetTokenAccumulationLosesNoUpdates() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties props =
                new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties(
                        null, null, null, null, null);
        int instances = 2;
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threads)) {
            for (int i = 0; i < instances; i++) {
                HookEnvironment env = new HookEnvironment("sess-bd", "agent", store);
                TokenBudgetHook hook = new TokenBudgetHook(props, "m", null);
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        hook.afterModel(new StubModelCallContext(env, usageResponse(50, 50)));
                        return null;
                    }));
                }
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
        Map<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> all =
                store.getAll("sess-bd");
        assertThat(all.get("buzhou.budget.prompt-tokens").value())
                .isEqualTo(String.valueOf(instances * threads * 50L));
        assertThat(all.get("buzhou.budget.completion-tokens").value())
                .isEqualTo(String.valueOf(instances * threads * 50L));
    }

    // ---- fixtures ----

    private static ChatClientResponse usageResponse(int promptTokens, int completionTokens) {
        ChatResponse chat = new ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(
                new AssistantMessage("r"))), ChatResponseMetadata.builder()
                .usage(new DefaultUsage(promptTokens, completionTokens)).build());
        return new ChatClientResponse(chat, Map.of());
    }

    private static final class StubToolCallContext implements ToolCallContext {
        private final HookEnvironment env;
        private final AtomicInteger callSeq = new AtomicInteger();

        StubToolCallContext(HookEnvironment env) {
            this.env = env;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return 1; }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(SessionEvent event) { }
        @Override public String toolCallId() { return "tc-" + callSeq.incrementAndGet(); }
        @Override public String toolName() { return "noop_tool"; }
        @Override public Map<String, Object> arguments() { return Map.of(); }
        @Override public Object result() { return null; }
        @Override public Throwable error() { return null; }
        @Override public void replaceArguments(Map<String, Object> newArguments) { }
        @Override public void replaceResult(Object newResult) { }
    }

    private static final class StubModelCallContext implements ModelCallContext {
        private final HookEnvironment env;
        private final ChatClientResponse response;

        StubModelCallContext(HookEnvironment env, ChatClientResponse response) {
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
}
