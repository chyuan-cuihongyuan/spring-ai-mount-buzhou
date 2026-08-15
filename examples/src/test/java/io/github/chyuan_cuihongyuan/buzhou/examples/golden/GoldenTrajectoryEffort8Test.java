package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.EventSequenceAssert;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹 C（spec 38 §C / T137 / impl-110）：effort #8 新机制——半开多探测、
 * 目录溢出+检索、死信重放、保留清扫。
 */
class GoldenTrajectoryEffort8Test {

    // ---- G13 半开多探测（阈值 2） ----

    /** 跳闸→探测 1 成功仍 HALF_OPEN→探测 2 成功才 CLOSED（state-changed 轨迹三段）。 */
    @Test
    void g13HalfOpenMultiProbeTrajectory() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError());
        model.enqueueThrow(networkError());
        model.enqueueText("probe-1-ok");
        model.enqueueText("probe-2-ok");
        ResilienceProperties props = new ResilienceProperties(true, 2,
                Duration.ofMillis(1), Duration.ofMillis(20), 2.0, 0.0, null,
                Duration.ofSeconds(5), null,
                new ResilienceProperties.Circuit(null, 4, 1, 0.5, Duration.ofMillis(80),
                        null, null, 2), // half-open-success-threshold=2
                null, null);
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(props, "g13", new ResilienceStats(), List.of()));
        AgentSession session = runtime.spawn("app", "ag", "g13");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        try {
            session.chat("q1"); // 2 次失败 → 跳闸 OPEN
        } catch (RuntimeException expected) {
        }
        Thread.sleep(120); // 过冷却
        assertThat(session.chat("probe-1")).isEqualTo("probe-1-ok"); // 探测 1：仍 HALF_OPEN
        assertThat(session.chat("probe-2")).isEqualTo("probe-2-ok"); // 探测 2：CLOSED

        List<String> states = events.events().stream()
                .filter(e -> "circuit.state-changed".equals(e.type()))
                .map(e -> String.valueOf(e.payload().get("to")))
                .toList();
        assertThat(states).containsSubsequence("OPEN", "HALF_OPEN", "CLOSED"); // 三段轨迹
        session.close();
    }

    // ---- G14 目录溢出 + skill_search ----

    /** 目录截断提示溢出；skill_search 检索源走不截断全集（不受注入上限限制）。 */
    @Test
    void g14CatalogOverflowAndSearch() {
        io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule module =
                io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule.builder().build();
        var registry = module.skillRegistry();
        var search = new io.github.chyuan_cuihongyuan.buzhou.skill.SkillSearchTool(registry, null);

        String result = search.call("{\"query\":\"sql\"}", null);

        assertThat(result).contains("sql-tuning").contains("load_skill(name)");
        // 检索源（不截断）≥ 注入面（截断）
        assertThat(registry.listAllFor(null, null).size())
                .isGreaterThanOrEqualTo(registry.listForPage(null, null).entries().size());
    }

    // ---- G15 死信重放 ----

    /** 恒 500 耗尽→死信；端点恢复→replayDeadLetters 一键补投（终见事件）。 */
    @Test
    void g15DeadLetterReplayTrajectory() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        AtomicInteger[] status = {new AtomicInteger(500)};
        java.util.concurrent.ConcurrentLinkedQueue<String> received =
                new java.util.concurrent.ConcurrentLinkedQueue<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            hits.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes());
            if (status[0].get() == 200) {
                received.add(body);
            }
            exchange.sendResponseHeaders(status[0].get(), -1);
            exchange.close();
        });
        server.start();
        InMemorySessionStateStore shared = new InMemorySessionStateStore();
        WebhookEventForwarder forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofSeconds(2), 2, 100, null), shared);
        forwarder.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "g15.die", Map.of()));
        await(() -> forwarder.deadLettered() == 1);
        assertThat(forwarder.deadLetters()).hasSize(1);

        status[0].set(200);
        assertThat(forwarder.replayDeadLetters()).isEqualTo(1); // 一键重放
        await(() -> forwarder.delivered() == 1);
        await(() -> forwarder.pendingCount() == 0);
        assertThat(received.peek()).contains("g15.die"); // 消费端终见
        forwarder.close();
        server.stop(0);
    }

    // ---- G16 保留清扫 ----

    /** 过期 CLOSED 淘汰、ACTIVE 永不扫、未过期保留。 */
    @Test
    void g16RetentionPurgeTrajectory() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(new SessionInfo("g16-live", "app", "ag", SessionInfo.STATUS_ACTIVE,
                1L, 100L, 1, Map.of())); // 老但活跃
        index.upsert(new SessionInfo("g16-closed", "app", "ag", SessionInfo.STATUS_CLOSED,
                1L, 100L, 1, Map.of())); // 过期关闭
        index.upsert(new SessionInfo("g16-fresh", "app", "ag", SessionInfo.STATUS_CLOSED,
                1L, System.currentTimeMillis(), 1, Map.of())); // 新关闭

        int purged = index.purgeOlderThan(java.time.Instant.ofEpochMilli(1_000L), 10);

        assertThat(purged).isEqualTo(1);
        assertThat(index.list(SessionIndexQuery.defaults()))
                .extracting(SessionInfo::sessionId)
                .containsExactlyInAnyOrder("g16-live", "g16-fresh");
    }

    // ---- helpers ----

    private static UncheckedIOException networkError() {
        return new UncheckedIOException(new java.io.IOException("connection reset"));
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }

}
