package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * dashboard 响应 gzip 测试（spec 1634 / T2419–T2420 / impl 1187）：
 * Accept-Encoding: gzip 且响应超阈值 → Content-Encoding: gzip（体可解压回 JSON）；
 * 无 Accept-Encoding 恒明文；小响应不压缩。
 */
class DashboardGzipTest {

    private ObservabilityStore store;
    private DashboardModule dashboard;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() {
        store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().observabilityStore();
        Instant t0 = Instant.parse("2026-08-08T10:00:00Z");
        // 大量 span——列表响应远超 gzip 阈值
        for (int i = 0; i < 80; i++) {
            store.saveSpans(List.of(new SpanRecord("s" + i, null, "sess-" + i, -1,
                    "SESSION", "session", t0.plusSeconds(i), t0.plusSeconds(i + 1), "OK",
                    Map.of("agent.name", "demo-agent-" + i, "idx", i))));
        }
        dashboard = DashboardModule.builder(store).port(0).build().start();
        client = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + dashboard.actualPort() + dashboard.pathPrefix();
    }

    @AfterEach
    void tearDown() {
        dashboard.close();
    }

    @Test
    void gzipAcceptedAndBodyDecompresses() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/sessions?size=100"))
                .header("Accept-Encoding", "gzip").GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.headers().firstValue("Content-Encoding")).contains("gzip");
        try (GZIPInputStream gz = new GZIPInputStream(
                new ByteArrayInputStream(response.body()))) {
            byte[] inflated = gz.readAllBytes();
            assertThat(inflated.length).isGreaterThan(response.body().length); // 压缩生效
            assertThat(new String(inflated)).contains("sess-79");
        }
    }

    @Test
    void noAcceptEncodingStaysPlain() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/sessions?size=100")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
        assertThat(response.body()).contains("sess-79");
    }
}
