package io.github.chyuan_cuihongyuan.buzhou.guard.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.EventDeduplicator;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.EventSchemaChecker;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.SequenceFence;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookFanout;
import io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules;
import io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiEventRedactor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 211 / T585：webhook 族组合 E2E——链序定案
 * schema（坏事件最先拦）→ dedup（重复不浪费脱敏）→ redactor（出站前最后脱）
 * → fanout（双 sink 路由）。五联断言：拦坏/吞重/脱敏/路由/fence 序连。
 * 位于 guard 模块（redactor 在此；guard 依赖 core 可见全族）。
 */
class WebhookPipelineE2E {

    private HttpServer auditServer;
    private HttpServer alarmServer;
    private WebhookFanout fanout;

    private final ConcurrentLinkedQueue<String> auditBodies = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> alarmBodies = new ConcurrentLinkedQueue<>();

    @AfterEach
    void tearDown() {
        if (fanout != null) {
            fanout.close();
        }
        if (auditServer != null) {
            auditServer.stop(0);
        }
        if (alarmServer != null) {
            alarmServer.stop(0);
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    private HttpServer sink(ConcurrentLinkedQueue<String> bodies) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        return server;
    }

    private WebhookEventForwarder forwarder(HttpServer server) {
        return new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(2000), 2, 64, null),
                new InMemorySessionStateStore());
    }

    @Test
    void fiveAssertionsOverFullPipeline() throws Exception {
        auditServer = sink(auditBodies);
        alarmServer = sink(alarmBodies);
        WebhookEventForwarder auditSink = forwarder(auditServer);
        WebhookEventForwarder alarmSink = forwarder(alarmServer);
        alarmSink.setIncludeTypes(List.of("session.error"));

        // 组装：fanout ← redactor（最贴出站）← dedup ← schema（最外）
        WebhookFanout inner = new WebhookFanout(List.of(auditSink, alarmSink));
        PiiEventRedactor redactor = new PiiEventRedactor(inner, null, new CustomPiiRules(
                List.of(CustomPiiRules.Rule.of("ORDER_ID", "ORD-\\d{6,}"))));
        EventDeduplicator dedup = new EventDeduplicator(redactor);
        EventSchemaChecker pipeline = new EventSchemaChecker(dedup,
                Map.of("user.turn.completed", Set.of("sessionId"),
                        "session.error", Set.of("sessionId", "code")));
        fanout = inner;

        // ① 坏事件（缺 code）——schema 最外拦，两 sink 均不达
        pipeline.onEvent(SessionEvent.of("session.error", Map.of("sessionId", "s1")));

        // ② 重复事件——dedup 吞，audit 只达一次
        Map<String, Object> turnPayload = Map.of("sessionId", "s1",
                "input", "查 ORD-20260001 电话 13812345678");
        pipeline.onEvent(SessionEvent.of("user.turn.completed", turnPayload));
        pipeline.onEvent(SessionEvent.of("user.turn.completed", turnPayload));

        // ③ 合法错误事件——双 sink 都达
        pipeline.onEvent(SessionEvent.of("session.error",
                Map.of("sessionId", "s1", "code", 500)));

        await(() -> auditBodies.size() >= 2);
        await(() -> !alarmBodies.isEmpty());

        // ① 断言：无缺键事件出站
        assertThat(auditBodies).noneMatch(
                b -> b.contains("session.error") && !b.contains("\"code\""));
        // ② 断言：重复只达一次
        assertThat(auditBodies.stream().filter(b -> b.contains("user.turn.completed")).count())
                .isEqualTo(1);
        // ③ 断言：PII 出站已脱（内置电话 + 自定义订单号）
        String turnBody = auditBodies.stream()
                .filter(b -> b.contains("user.turn.completed")).findFirst().orElseThrow();
        assertThat(turnBody).contains("[PII:ORDER_ID]").contains("[PII:CN_PHONE]");
        assertThat(turnBody).doesNotContain("ORD-20260001").doesNotContain("13812345678");
        // ④ 断言：类型路由——alarm 只收错误族且含必备键
        assertThat(alarmBodies).hasSize(1);
        assertThat(alarmBodies.peek()).contains("session.error").contains("\"code\"");
        // ⑤ 断言：fence 序连——audit seq 单调无 GAP
        SequenceFence fence = new SequenceFence();
        auditBodies.stream().map(WebhookPipelineE2E::seqOf).sorted().forEach(
                seq -> assertThat(fence.observe("audit", seq))
                        .isNotEqualTo(SequenceFence.Verdict.GAP));
    }

    @Test
    void layerRemovalKeepsPipelineAssemblable() throws Exception {
        // smoke：去掉 dedup/redactor 层，schema 直连 fanout 仍可装配投递
        auditServer = sink(auditBodies);
        WebhookEventForwarder auditSink = forwarder(auditServer);
        WebhookFanout inner = new WebhookFanout(List.of(auditSink));
        EventSchemaChecker pipeline = new EventSchemaChecker(inner,
                Map.of("t.ok", Set.of("k")));
        fanout = inner;

        pipeline.onEvent(SessionEvent.of("t.ok", Map.of("k", "v")));
        await(() -> !auditBodies.isEmpty());
        assertThat(auditBodies.peek()).contains("t.ok");
    }

    private static long seqOf(String body) {
        int idx = body.indexOf("\"seq\":");
        int start = idx + "\"seq\":".length();
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Long.parseLong(body.substring(start, end));
    }
}
