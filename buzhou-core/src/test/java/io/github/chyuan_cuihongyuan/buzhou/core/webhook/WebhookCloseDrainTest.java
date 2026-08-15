package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 44 §A / T159 / impl-130：webhook close 排空语义钉住——close 等待在途投递收尾并排空
 * 「已到期」记录（预算可配 close-drain-timeout，缺省 5s）；未到期退避记录仍留存 store。
 */
class WebhookCloseDrainTest {

    private HttpServer server;
    private WebhookEventForwarder forwarder;

    @AfterEach
    void stop() {
        if (forwarder != null) {
            forwarder.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void closeWaitsForInFlightAndDrainsDueRecords() throws Exception {
        CountDownLatch firstInFlight = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger hits = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            hits.incrementAndGet();
            if (hits.get() == 1) {
                firstInFlight.countDown();
                try {
                    releaseFirst.await(10, TimeUnit.SECONDS); // 首个请求挂住（在途未决）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        SessionStateStore store = new InMemorySessionStateStore();
        forwarder = new WebhookEventForwarder(new BuzhouWebhookProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/hook",
                null, Duration.ofMillis(2000), 2, 100, null, Duration.ofSeconds(8)), store);

        forwarder.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent
                .of("evt.a", Map.of("sessionId", "s1")));
        assertThat(firstInFlight.await(10, TimeUnit.SECONDS)).isTrue(); // A 在途
        forwarder.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent
                .of("evt.b", Map.of("sessionId", "s1"))); // B 已到期、排队待投

        // close 在 A 在途期间发起：有界等待在途收尾（预算 8s）→ 排空已到期 B → 才返回
        Thread closer = new Thread(forwarder::close);
        closer.start();
        Thread.sleep(300); // 让 close 进入等待（不先硬截断）
        releaseFirst.countDown(); // 放行 A
        closer.join(15_000);

        assertThat(forwarder.delivered()).isEqualTo(2); // A 与 B 都送达（B 由 close 排空投出）
        assertThat(forwarder.pendingCount()).isZero();
    }
}
