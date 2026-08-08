package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SSRF 拦截（ticket 16 验收：内网地址被拦）+ http_request 请求机制。
 */
class HttpRequestToolTest {

    @Test
    void privateRangesBlocked() {
        SsrfGuard guard = SsrfGuard.defaults();
        for (String host : new String[]{"127.0.0.1", "10.1.2.3", "172.16.5.5", "192.168.1.1",
                "169.254.169.254", "0.0.0.0", "::1", "localhost"}) {
            assertThat(guard.check(host)).as("应拦截: %s", host).isNotNull();
        }
    }

    @Test
    void metadataEndpointBlocked() {
        String result = new HttpRequestTool(SsrfGuard.defaults(), Duration.ofSeconds(5))
                .call("{\"method\":\"GET\",\"url\":\"http://169.254.169.254/latest/meta-data/\"}");
        assertThat(result).contains("拒绝").contains("SSRF");
    }

    @Test
    void allowlistBypassesBlock() {
        SsrfGuard guard = new SsrfGuard(true, List.of("127.0.0.0/8"));
        assertThat(guard.check("127.0.0.1")).isNull();
        // 主机名放行
        SsrfGuard byHost = new SsrfGuard(true, List.of("internal.corp.example"));
        assertThat(byHost.check("internal.corp.example")).isNull();
    }

    @Test
    void unresolvableHostRejectedFailClosed() {
        assertThat(SsrfGuard.defaults().check("nonexistent.invalid.tld.buzhou")).isNotNull();
    }

    @Test
    void blockDisabledAllowsPrivate() {
        assertThat(new SsrfGuard(false, List.of()).check("127.0.0.1")).isNull();
    }

    @Test
    void invalidCidrPrefixRejected() {
        // 前缀超地址位数：配置错误应显式失败，而非放行后 matches 越界
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new SsrfGuard(true, List.of("1.2.3.4/40")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestMechanicsAgainstLocalServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] body = ("echo:" + exchange.getRequestMethod()
                    + ":" + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            // SSRF 放行本机（测试通道），验证请求机制本身
            HttpRequestTool tool = new HttpRequestTool(
                    new SsrfGuard(true, List.of("127.0.0.0/8")), Duration.ofSeconds(5));
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/echo";

            String get = tool.call("{\"method\":\"GET\",\"url\":\"" + url + "\"}");
            assertThat(get).startsWith("HTTP 200").contains("echo:GET:");

            String post = tool.call("{\"method\":\"POST\",\"url\":\"" + url + "\","
                    + "\"headers\":{\"X-Test\":\"1\"},\"body\":\"载荷\"}");
            assertThat(post).startsWith("HTTP 200").contains("echo:POST:载荷");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsBadMethodAndScheme() {
        HttpRequestTool tool = new HttpRequestTool(new SsrfGuard(false, List.of()),
                Duration.ofSeconds(5));
        assertThat(tool.call("{\"method\":\"TRACE\",\"url\":\"http://x\"}"))
                .contains("不支持的 method");
        assertThat(tool.call("{\"method\":\"GET\",\"url\":\"file:///etc/passwd\"}"))
                .contains("仅支持 http/https");
    }
}
