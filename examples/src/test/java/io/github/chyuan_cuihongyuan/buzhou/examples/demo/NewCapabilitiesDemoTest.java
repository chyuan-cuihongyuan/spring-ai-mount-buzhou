package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.budget.TokenBudgetHook;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.StructuredOutputException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * effort #5 新能力端到端演示（T98 / impl-73）：熔断降级链 / token 预算闸 / 会话 fork /
 * webhook 事件外发 / 结构化输出 REASK / 日配额——全部经完整会话管线驱动，作为 examples
 * 接缝文档（各机制模块另有细粒度测试）。
 */
class NewCapabilitiesDemoTest {

    private static final Duration FAST = Duration.ofMillis(1);
    private static final Duration FAST_MAX = Duration.ofMillis(20);

    /** 每调用附带 usage（100 prompt + 50 completion）的替身。 */
    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(100, 50)).build());
        }
    }

    /** 1) 降级链：主模型 NETWORK 耗尽 → 备模型接管，用户拿到备模型回复。 */
    @Test
    void fallbackChainKeepsServiceAlive() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError());
        primary.enqueueThrow(networkError());
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueText("from-backup-model");
        ResilienceProperties props = new ResilienceProperties(true, 2, FAST, FAST_MAX,
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null);

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props, "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("backup", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "fallback-demo");

        assertThat(session.chat("hi")).isEqualTo("from-backup-model");
        session.close();
    }

    /** 2) token 预算闸：累计触顶后下一轮被拦截（回复含预算说明，模型零调用）。 */
    @Test
    void tokenBudgetStopsSessionWhenCapReached() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 每轮 150 tokens；上限 200：第一轮后 150 < 200 放行第二轮 → 300；第三轮拦截
        BuzhouTokenBudgetProperties budget = new BuzhouTokenBudgetProperties(
                null, null, 200L, null, null);
        AgentRuntime runtime = Buzhou.runtime(model, stores, new RuntimeConfig(
                List.of(new TokenBudgetHook(budget, "demo", stores.observabilityStore())),
                Set.of(), Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "agent", "budget-demo");

        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).isEqualTo("r2");
        assertThat(session.chat("q3")).contains("预算上限");
        assertThat(model.seenPrompts).hasSize(2);
        session.close();
    }

    /** 3) 会话 fork：分支继承完整历史、预算重置（fork = 重试语义）。 */
    @Test
    void forkBranchesSessionWithFreshBudget() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("a1");
        model.enqueueText("b1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouTokenBudgetProperties budget = new BuzhouTokenBudgetProperties(
                null, null, 150L, null, null); // 恰好一轮
        AgentRuntime runtime = Buzhou.runtime(model, stores, new RuntimeConfig(
                List.of(new TokenBudgetHook(budget, "demo", stores.observabilityStore())),
                Set.of(), Set.of(), null, List.of()));

        AgentSession source = runtime.spawn("app", "agent", "fork-src");
        assertThat(source.chat("q1")).isEqualTo("a1"); // 预算耗尽
        source.close();

        AgentSession branch = runtime.fork("fork-src", "app", "agent", "fork-branch");
        assertThat(branch.chat("q2")).isEqualTo("b1"); // 分支预算重置
        assertThat(stores.messageStore().load("fork-src")).hasSize(2); // 源不动
        branch.close();
    }

    /** 4) webhook 事件外发：会话事件（session.forked 等）经签名投递到外部端点。 */
    @Test
    void webhookReceivesSignedEvents() throws Exception {
        java.util.concurrent.ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        List<String> signatures = new java.util.concurrent.CopyOnWriteArrayList<>();
        HttpServer server = startCaptureServer(received, signatures);
        WebhookEventForwarder forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "demo-secret", Duration.ofSeconds(3), 2, 32));

        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = (DefaultAgentRuntime) Buzhou.runtime(model, stores,
                RuntimeConfig.defaults());
        runtime.addGlobalEventListener(forwarder);
        AgentSession source = runtime.spawn("app", "agent", "wh-src");
        source.chat("q1");
        source.close();
        runtime.fork("wh-src", "app", "agent", "wh-branch").close();

        await(() -> received.stream().anyMatch(b -> b.contains("session.forked")));
        assertThat(received).isNotEmpty();
        assertThat(signatures).allSatisfy(s -> assertThat(s).hasSize(64)); // HMAC-SHA256 hex
        forwarder.close();
        server.stop(0);
    }

    /** 5) 结构化输出：首轮废话 REASK 一次后返回合法实体。 */
    @Test
    void structuredOutputReasksOnceAndRecovers() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("我不想要 JSON。");
        model.enqueueText("{\"answer\":42}");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "structured-demo");

        Answer a = session.chatForEntity("the answer?", Answer.class);
        assertThat(a.answer()).isEqualTo(42);
        assertThat(model.seenPrompts).hasSize(2);
        session.close();
    }

    /** 6) 结构化输出两败：抛 StructuredOutputException（含两轮摘要）。 */
    @Test
    void structuredOutputFailsLoudlyAfterReask() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("nope");
        model.enqueueText("still nope");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "structured-fail");

        assertThatThrownBy(() -> session.chatForEntity("q", Answer.class))
                .isInstanceOf(StructuredOutputException.class)
                .hasMessageContaining("still nope");
        session.close();
    }

    /** 7) 日配额：turns-per-day=1，第二轮被拦。 */
    @Test
    void sessionQuotaCapsDailyTurns() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        ResilienceProperties.SessionQuota quota =
                new ResilienceProperties.SessionQuota(1, null, null);
        AgentRuntime runtime = Buzhou.runtime(model, stores, new RuntimeConfig(
                List.of(new SessionQuotaHook(quota, new ResilienceStats())),
                Set.of(), Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "agent", "quota-demo");

        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).contains("配额上限");
        assertThat(model.seenPrompts).hasSize(1);
        session.close();
    }

    // ---- helpers ----

    record Answer(long answer) {
    }

    private static UncheckedIOException networkError() {
        return new UncheckedIOException(new java.io.IOException("connection reset"));
    }

    private static HttpServer startCaptureServer(ConcurrentLinkedQueue<String> received,
            List<String> signatures) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(body);
            List<String> sig = exchange.getRequestHeaders().get("X-Buzhou-Signature");
            if (sig != null) {
                signatures.add(sig.get(0));
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待 webhook 投递超时");
            }
            Thread.sleep(20);
        }
    }
}
