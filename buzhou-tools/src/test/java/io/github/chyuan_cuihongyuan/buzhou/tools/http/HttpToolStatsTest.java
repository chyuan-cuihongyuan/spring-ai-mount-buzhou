package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1049 / impl 801：http_request 请求量水位与结果分布——本地回环成功（successes）、
 * method/url/ssrf/timeout/oversize 五类显式拒绝、五桶守恒恒等式、resetForTest 归零。
 * HttpServer 回环 + 放行清单 127.0.0.0/8（既有 HttpRequestToolTest 先例），零外网依赖。
 */
class HttpToolStatsTest {

    @BeforeEach
    void reset() {
        HttpRequestTool.resetForTest();
        SsrfGuard.resetForTest();
    }

    private HttpRequestTool tool() {
        return new HttpRequestTool(new SsrfGuard(true, List.of("127.0.0.0/8")),
                Duration.ofSeconds(5));
    }

    private HttpServer echoServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] body = "echo".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }

    @Test
    void successfulRequestCountsSuccess() throws Exception {
        HttpServer server = echoServer();
        try {
            String out = tool().call("{\"method\":\"GET\",\"url\":\"http://127.0.0.1:"
                    + server.getAddress().getPort() + "/echo\"}");
            assertThat(out).startsWith("HTTP 200");
        } finally {
            server.stop(0);
        }

        HttpRequestTool.HttpToolStats stats = HttpRequestTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.successes()).isEqualTo(1);
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void badMethodCountsItsBucket() {
        String out = tool().call("{\"method\":\"TRACE\",\"url\":\"http://example.com/\"}");
        assertThat(out).contains("不支持的 method");

        assertThat(HttpRequestTool.stats().methodRejects()).isEqualTo(1);
    }

    @Test
    void badUrlAndSchemeCountUrlBucket() {
        HttpRequestTool tool = tool();
        // 非法 URL（URI.create 失败）：含空格
        assertThat(tool.call("{\"method\":\"GET\",\"url\":\"http://bad url/\"}"))
                .contains("非法 URL");
        // 非法 scheme
        assertThat(tool.call("{\"method\":\"GET\",\"url\":\"ftp://example.com/\"}"))
                .contains("仅支持 http/https");

        assertThat(HttpRequestTool.stats().urlRejects()).isEqualTo(2);
        assertThat(HttpRequestTool.stats().successes()).isZero();
    }

    @Test
    void ssrfRejectionCountsItsBucket() {
        // 内网地址且守卫无放行 → SSRF 拒绝
        HttpRequestTool strict = new HttpRequestTool(new SsrfGuard(true, List.of()),
                Duration.ofSeconds(5));
        String out = strict.call("{\"method\":\"GET\",\"url\":\"http://10.1.2.3/x\"}");
        assertThat(out).contains("拒绝");

        assertThat(HttpRequestTool.stats().ssrfRejects()).isEqualTo(1);
        assertThat(HttpRequestTool.stats().successes()).isZero();
    }

    @Test
    void badTimeoutCountsItsBucket() {
        String out = tool().call(
                "{\"method\":\"GET\",\"url\":\"http://93.184.216.34/\",\"timeoutSeconds\":0}");
        assertThat(out).contains("timeoutSeconds 超出允许范围");

        assertThat(HttpRequestTool.stats().timeoutParamRejects()).isEqualTo(1);
    }

    @Test
    void oversizeResponseCountsItsBucket() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/big", exchange -> {
            // Content-Length 预检路径：声明 8MB+1，响应体超读入上限
            exchange.sendResponseHeaders(200, HttpRequestTool.MAX_RESPONSE_BYTES + 1);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[1]);
            }
        });
        server.start();
        try {
            String out = tool().call("{\"method\":\"GET\",\"url\":\"http://127.0.0.1:"
                    + server.getAddress().getPort() + "/big\"}");
            assertThat(out).contains("超过读入上限");
        } finally {
            server.stop(0);
        }

        assertThat(HttpRequestTool.stats().oversizeRejects()).isEqualTo(1);
        assertThat(HttpRequestTool.stats().successes()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() throws Exception {
        HttpServer server = echoServer();
        try {
            HttpRequestTool tool = tool();
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/echo";
            tool.call("{\"method\":\"GET\",\"url\":\"" + url + "\"}");   // successes
            tool.call("{\"method\":\"TRACE\",\"url\":\"" + url + "\"}"); // method 拒
            tool.call("{\"method\":\"GET\",\"url\":\"ftp://x/\"}");      // url 拒
            tool.call("{\"method\":\"GET\",\"timeoutSeconds\":0,\"url\":\"" + url + "\"}"); // timeout 拒

            HttpRequestTool.HttpToolStats stats = HttpRequestTool.stats();
            assertThat(stats.attempts()).isEqualTo(4);
            assertThat(stats.attempts())
                    .isEqualTo(stats.successes() + stats.totalRejects());
            assertThat(stats.successes()).isEqualTo(1);
            assertThat(stats.methodRejects()).isEqualTo(1);
            assertThat(stats.urlRejects()).isEqualTo(1);
            assertThat(stats.timeoutParamRejects()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resetForTestZeroesCounters() {
        tool().call("{\"method\":\"TRACE\",\"url\":\"http://x/\"}");
        assertThat(HttpRequestTool.stats().attempts()).isEqualTo(1);

        HttpRequestTool.resetForTest();

        HttpRequestTool.HttpToolStats stats = HttpRequestTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.successes()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
