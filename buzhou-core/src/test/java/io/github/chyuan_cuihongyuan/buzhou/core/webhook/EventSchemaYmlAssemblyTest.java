package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 307 / impl-330：事件 schema yml 声明装配回归——Map 绑定（含点号类型键）/
 * fail-closed 坏事件不投 / fail-open 违规也投 / 挂点去重 / 默认无 checker。
 */
class EventSchemaYmlAssemblyTest {

    private HttpServer server;
    private final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private ApplicationContextRunner runnerWithSchema(String... extra) {
        List<String> props = new java.util.ArrayList<>(List.of(
                "buzhou.webhook.url=http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                "buzhou.webhook.timeout=2s"));
        props.addAll(List.of(extra));
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(props.toArray(String[]::new));
    }

    private static SessionEvent event(String type, Map<String, Object> payload) {
        return new SessionEvent(type, payload, Instant.now());
    }

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }

    @Test
    void schemaBindsFromYmlIncludingDottedTypeKeys() {
        runnerWithSchema(
                "buzhou.webhook.schema.required-keys.[turn.completed]=sessionId,turnNo",
                "buzhou.webhook.schema.fail-open=true").run(context -> {
            BuzhouWebhookProperties props = context.getBean(BuzhouWebhookProperties.class);
            assertThat(props.schema().requiredKeys())
                    .containsEntry("turn.completed", List.of("sessionId", "turnNo"));
            assertThat(props.schema().failOpen()).isTrue();
            assertThat(context).hasBean("buzhouEventSchemaChecker");
        });
    }

    @Test
    void failClosedDropsViolatingEvents_andPassesGoodOnes() throws Exception {
        runnerWithSchema("buzhou.webhook.schema.required-keys.[turn.completed]=sessionId").run(context -> {
            SessionEventListener checker = context.getBean("buzhouEventSchemaChecker",
                    SessionEventListener.class);
            checker.onEvent(event("turn.completed", Map.of())); // 缺 sessionId
            checker.onEvent(event("turn.completed", Map.of("sessionId", "s-1"))); // 合格
            checker.onEvent(event("undeclared.type", Map.of())); // open-world 放行
        });
        await(() -> received.size() >= 2);
        assertThat(received).hasSize(2);
        assertThat(received.stream().filter(b -> b.contains("s-1"))).hasSize(1);
        assertThat(received.stream().filter(b -> b.contains("undeclared.type"))).hasSize(1);
    }

    @Test
    void failOpenDeliversViolationsForObservation() throws Exception {
        runnerWithSchema(
                "buzhou.webhook.schema.required-keys.[turn.completed]=sessionId",
                "buzhou.webhook.schema.fail-open=true").run(context -> {
            SessionEventListener checker = context.getBean("buzhouEventSchemaChecker",
                    SessionEventListener.class);
            checker.onEvent(event("turn.completed", Map.of())); // 违规也投（观察模式）
        });
        await(() -> !received.isEmpty());
        assertThat(received).hasSize(1);
    }

    @Test
    void noDeclarationRegistersNoChecker() {
        runnerWithSchema().run(context ->
                assertThat(context).doesNotHaveBean(EventSchemaChecker.class));
    }

    @Test
    void effectiveGlobalListenersDedupesWrappedDelegates() {
        RecordingListener forwarder = new RecordingListener();
        RecordingListener other = new RecordingListener();
        EventSchemaChecker checker = new EventSchemaChecker(forwarder,
                Map.of("t", java.util.Set.of("k")));
        List<SessionEventListener> effective =
                io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration
                        .effectiveGlobalListeners(List.of(forwarder, checker, other));
        assertThat(effective).containsExactly(checker, other); // forwarder 被 checker 代投
        // 无 checker 时原样
        assertThat(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration
                .effectiveGlobalListeners(List.of(forwarder, other)))
                .containsExactly(forwarder, other);
    }

    private static final class RecordingListener implements SessionEventListener {
        @Override
        public void onEvent(SessionEvent event) {
        }
    }
}
