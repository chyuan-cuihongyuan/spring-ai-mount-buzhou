package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1061 / impl 813：Dashboard HTTP 状态分布读面——正常请求（ok）、
 * 401 鉴权/404 未知路径/400 坏参数结局桶、守恒恒等式、resetForTest 归零。
 * 骨架同 DashboardHttpServerTest（DashboardModule + 随机端口回环）。
 */
class DashboardHttpStatsTest {

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore store;
    private DashboardModule dashboard;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() {
        DashboardHttpServer.resetForTest();
        store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().observabilityStore();
        store.appendSpan(SpanRecord.builder().sessionId("s1").spanId("sp1")
                .turnSeq(1).startedAt(Instant.now()).build());
        dashboard = DashboardModule.builder(store)
                .port(0)
                .build()
                .start();
        client = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + dashboard.actualPort() + dashboard.pathPrefix();
    }

    @AfterEach
    void tearDown() {
        dashboard.close();
    }

    private int get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void successfulRequestCountsOk() throws Exception {
        assertThat(get("/api/sessions")).isEqualTo(200);

        DashboardHttpServer.DashboardHttpStats stats = DashboardHttpServer.stats();
        assertThat(stats.requests()).isEqualTo(1);
        assertThat(stats.ok()).isEqualTo(1);
        assertThat(stats.authRejects()).isZero();
    }

    @Test
    void unknownPathCountsNotFound() throws Exception {
        assertThat(get("/api/no-such-endpoint")).isEqualTo(404);

        assertThat(DashboardHttpServer.stats().notFounds()).isEqualTo(1);
    }

    @Test
    void badParamCountsBadRequest() throws Exception {
        // 分页参数非数字等坏参数 → 400
        int code = get("/api/sessions?page=abc");
        assertThat(code).isEqualTo(400);

        assertThat(DashboardHttpServer.stats().badRequests()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedRequests() throws Exception {
        get("/api/sessions");          // ok
        get("/api/no-such-endpoint");  // 404
        get("/api/sessions?page=abc"); // 400

        DashboardHttpServer.DashboardHttpStats stats = DashboardHttpServer.stats();
        assertThat(stats.requests()).isEqualTo(3);
        assertThat(stats.requests())
                .isEqualTo(stats.ok() + stats.authRejects() + stats.badRequests()
                        + stats.notFounds() + stats.tooLarges() + stats.unimplemented()
                        + stats.serverErrors());
        assertThat(stats.ok()).isEqualTo(1);
        assertThat(stats.notFounds()).isEqualTo(1);
        assertThat(stats.badRequests()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() throws Exception {
        get("/api/sessions");
        assertThat(DashboardHttpServer.stats().requests()).isEqualTo(1);

        DashboardHttpServer.resetForTest();

        DashboardHttpServer.DashboardHttpStats stats = DashboardHttpServer.stats();
        assertThat(stats.requests()).isZero();
        assertThat(stats.ok()).isZero();
    }
}
