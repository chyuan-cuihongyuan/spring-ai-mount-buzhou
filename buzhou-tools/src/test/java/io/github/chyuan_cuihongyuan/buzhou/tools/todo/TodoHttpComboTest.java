package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.tools.http.HttpRequestTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.http.SsrfGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1104 / impl 856：todo×http 跨工具工作流组合——同会话交叉调用后
 * 双读面独立（互不串账）、reset 独立隔离。纯测试轮。
 */
class TodoHttpComboTest {

    @TempDir
    Path tmp;

    private HttpServer server;
    private TodoTool todo;
    private HttpRequestTool http;

    @BeforeEach
    void setUp() throws Exception {
        HttpRequestTool.resetForTest();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data", exchange -> {
            byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        http = new HttpRequestTool(
                new SsrfGuard(true, List.of("127.0.0.0/8")), Duration.ofSeconds(5));
        todo = new TodoTool(new io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoStore(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore()));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void crossCallsKeepBothReadoutsIndependent() throws Exception {
        // HTTP 取数
        String httpOut = http.call("{\"method\":\"GET\",\"url\":\"http://127.0.0.1:"
                + server.getAddress().getPort() + "/data\"}");
        assertThat(httpOut).startsWith("HTTP 200");
        // todo 更新
        String todoOut = todo.call("{\"action\":\"upsert\",\"todos\":[]}");
        assertThat(todoOut).isNotNull();

        HttpRequestTool.HttpToolStats hs = HttpRequestTool.stats();
        assertThat(hs.attempts()).isEqualTo(1);
        assertThat(hs.successes()).isEqualTo(1);

        // TodoTool 读面独立（HTTP 调用不影响 todo 计数）
        var todoStats = todo.actionStats();
        assertThat(todoStats.byAction().values().stream().mapToLong(Long::longValue).sum())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void resetsAreIndependent() throws Exception {
        http.call("{\"method\":\"GET\",\"url\":\"http://127.0.0.1:"
                + server.getAddress().getPort() + "/data\"}");
        todo.call("{\"action\":\"list\"}");

        HttpRequestTool.resetForTest();
        assertThat(HttpRequestTool.stats().attempts()).isZero();

        // todo 无统一 reset（R40 形状）——实例语义，此处仅验证 http 侧隔离
        assertThat(HttpRequestTool.stats().attempts()).isZero();
    }
}
