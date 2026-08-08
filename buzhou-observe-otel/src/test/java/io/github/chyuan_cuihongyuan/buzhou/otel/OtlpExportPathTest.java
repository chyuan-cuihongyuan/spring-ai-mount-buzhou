package io.github.chyuan_cuihongyuan.buzhou.otel;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 OTLP 导出路径（spec 03「OTel 导出桥」验收：OTLP 导出到 Collector）。
 *
 * <p>不依赖真实 Collector / Docker：起一个进程内 HTTP 接收器，模拟 Collector 的 OTLP HTTP receiver
 * （{@code POST /v1/traces}）。断言 {@link OtelBridge#otlp} 真实 {@code OtlpHttpSpanExporter} 能把
 * span 以合法 OTLP 请求送达、收到 200、body 非空。
 *
 * <p>「完整 trace 树」的内容正确性由 {@link OtelBridgeMappingTest} 以 {@code InMemorySpanExporter}
 * 断言；本测试聚焦「OTLP 真实导出链路可达」。真实 Collector 端到端（Docker 依赖）可后续以
 * {@code @Testcontainers(disabledWithoutDocker=true)} 形态补充，与 jdbc 模块同口径。
 */
class OtlpExportPathTest {

    @Test
    void otlpExporterDeliversToHttpReceiver() throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch delivered = new CountDownLatch(1);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/traces", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            synchronized (captured) {
                captured.write(body);
            }
            requests.incrementAndGet();
            delivered.countDown();
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/traces";

        try (OtelBridge bridge = OtelBridge.otlp(endpoint, OtelBridgeConfig.enabledDefaults())) {
            PipelineSink sink = bridge.sink();
            Instant start = Instant.ofEpochSecond(1000);
            sink.onSpan(new SpanRecord("s1", null, "sess-1", -1, SpanKind.SESSION, "session",
                    start, null, SpanStatus.RUNNING, Map.of()));
            sink.onSpan(new SpanRecord("s1", null, "sess-1", -1, SpanKind.SESSION, "session",
                    start, Instant.ofEpochSecond(1001), SpanStatus.OK, Map.of()));
            // close() → provider.shutdown() 强制 flush BatchSpanProcessor，触发 OTLP POST
        }

        assertThat(delivered.await(5, TimeUnit.SECONDS)).as("OTLP POST 应在 flush 后送达接收器").isTrue();
        assertThat(requests.get()).isGreaterThan(0);
        assertThat(captured.toByteArray()).isNotEmpty();
        server.stop(0);
    }
}
