package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 共享配额原子扣减并发红队（spec 56 §C / T251）：多「实例」（多 HookEnvironment /
 * 多 Hook 实例）共享同一 state store 并发递增——最终计数精确 = 递增次数（不丢更新）；
 * 日翻越竞争只重置一次；CAS 永久抢败时回退路径可观测（quotaCasFallbacks）。
 *
 * <p>内存 store 的 CAS 为 compute 原子（真原子路径）；JDBC/Redis 的 CAS 语义由
 * {@code AbstractBuzhouStoresContractTest#stateStoreCompareAndSwapIsConditional}
 * 三栈同测钉住。
 */
class SessionQuotaCasConcurrencyTest {

    private static final ResilienceProperties.SessionQuota HIGH_CAPS =
            new ResilienceProperties.SessionQuota(1_000_000, 1_000_000, 1_000_000L);

    /** 多实例并发 turns：4 实例 × 8 线程 × 25 次递增 = 800，最终计数精确无丢失。 */
    @Test
    void crossInstanceTurnIncrementsLoseNoUpdates() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        ResilienceStats stats = new ResilienceStats();
        int instances = 4;
        int threadsPerInstance = 8;
        int incrementsPerThread = 25;
        int total = instances * threadsPerInstance * incrementsPerThread;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threadsPerInstance)) {
            for (int i = 0; i < instances; i++) {
                HookEnvironment env = new HookEnvironment("sess-shared", "agent", store);
                SessionQuotaHook hook = new SessionQuotaHook(HIGH_CAPS, stats, Clock.systemUTC());
                for (int t = 0; t < threadsPerInstance; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        for (int n = 0; n < incrementsPerThread; n++) {
                            hook.beforeTurn(new StubTurnContext(env));
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
        String raw = store.get("sess-shared", "buzhou.quota.turns")
                .map(StateEntry::value).orElse(null);
        long today = java.time.LocalDate.now(Clock.systemUTC()).toEpochDay();
        assertThat(raw).isEqualTo(today + ":" + total); // 计数无丢失（旧读改写竞态已修）
        assertThat(stats.quotaCasFallbacks()).as("CAS 重试不应耗尽（回退次数）").isZero();
    }

    /** 日翻越竞争：预置旧日计数（day0:5），并发首写后从 0 精确计数（旧值不被继承也不分裂）。 */
    @Test
    void dayRolloverRaceResetsExactlyOnce() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        long today = java.time.LocalDate.now(Clock.systemUTC()).toEpochDay();
        store.put("sess-roll", new StateEntry("buzhou.quota.turns", "0:5", "hook", 1, null, Instant.now()));

        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            HookEnvironment env = new HookEnvironment("sess-roll", "agent", store);
            SessionQuotaHook hook = new SessionQuotaHook(HIGH_CAPS, null, Clock.systemUTC());
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    hook.beforeTurn(new StubTurnContext(env));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        }
        String raw = store.get("sess-roll", "buzhou.quota.turns")
                .map(StateEntry::value).orElse(null);
        assertThat(raw).isEqualTo(today + ":" + threads); // 旧日 5 不被继承；16 次递增全数入账
    }

    /** tokens 并发累计：双实例各 10 线程 × usage 100 → 当日合计精确 2000。 */
    @Test
    void crossInstanceTokenAccumulationIsExact() throws Exception {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        int instances = 2;
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new java.util.ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threads)) {
            for (int i = 0; i < instances; i++) {
                HookEnvironment env = new HookEnvironment("sess-tok", "agent", store);
                SessionQuotaHook hook = new SessionQuotaHook(HIGH_CAPS, null, Clock.systemUTC());
                for (int t = 0; t < threads; t++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        hook.afterModel(new StubModelCallContext(env, usageResponse(60, 40)));
                        return null;
                    }));
                }
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        }
        String raw = store.get("sess-tok", "buzhou.quota.tokens")
                .map(StateEntry::value).orElse(null);
        long today = java.time.LocalDate.now(Clock.systemUTC()).toEpochDay();
        assertThat(raw).isEqualTo(today + ":" + (instances * threads * 100L));
    }

    /** CAS 永久抢败（对抗 store：CAS 恒 false）→ 16 次重试耗尽走回退覆写 + 回退计数暴露。 */
    @Test
    void casExhaustionFallsBackWithObservableCounter() {
        ResilienceStats stats = new ResilienceStats();
        AdversarialCasStore store = new AdversarialCasStore();
        HookEnvironment env = new HookEnvironment("sess-fb", "agent", store);
        SessionQuotaHook hook = new SessionQuotaHook(
                new ResilienceProperties.SessionQuota(1_000_000, null, null), stats, Clock.systemUTC());

        hook.beforeTurn(new StubTurnContext(env));

        long today = java.time.LocalDate.now(Clock.systemUTC()).toEpochDay();
        assertThat(store.get("sess-fb", "buzhou.quota.turns"))
                .isPresent().get().extracting(StateEntry::value).isEqualTo(today + ":1"); // 回退仍完成递增
        assertThat(store.putCalls.get()).isEqualTo(1); // 走的是 fallback put 而非 CAS
        assertThat(stats.quotaCasFallbacks()).isEqualTo(1L); // 回退可观测
    }

    // ---- fixtures ----

    private static ChatClientResponse usageResponse(int promptTokens, int completionTokens) {
        ChatResponse chat = new ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(
                new AssistantMessage("r"))), ChatResponseMetadata.builder()
                .usage(new DefaultUsage(promptTokens, completionTokens)).build());
        return new ChatClientResponse(chat, java.util.Map.of());
    }

    private static final class StubTurnContext implements TurnContext {
        private final HookEnvironment env;
        private final AtomicInteger turn = new AtomicInteger();

        StubTurnContext(HookEnvironment env) {
            this.env = env;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return Math.max(turn.incrementAndGet(), 1); }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(SessionEvent event) { }
        @Override public String input() { return "q"; }
        @Override public String response() { return null; }
        @Override public void replaceInput(String newInput) { }
        @Override public void replaceResponse(String newResponse) { }
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

    /** 对抗 store：CAS 恒 false（模拟极端抢败），put 正常记数——钉回退路径语义。 */
    private static final class AdversarialCasStore extends InMemorySessionStateStore {
        final AtomicInteger putCalls = new AtomicInteger();

        @Override
        public boolean compareAndSwap(String sessionId, String key, String expectedValue, StateEntry update) {
            return false;
        }

        @Override
        public void put(String sessionId, StateEntry entry) {
            putCalls.incrementAndGet();
            super.put(sessionId, entry);
        }
    }
}
