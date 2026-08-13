package io.github.chyuan_cuihongyuan.buzhou.examples.redteam;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-20 / T48 红队 target：OpenAI 兼容端点形状的 Buzhou guard agent（测试作用域）——
 * guard 全开（taintTracking + injectionDefense + 危险工具 HITL 门）、确定性替身模型：
 * 红队评的是<b>护栏行为</b>（拦截/转确认/不越权），不评模型生成质量。
 *
 * <p>默认仅做 smoke 自检（服务起停 + 端点应答 guard 行为）；传 {@code -Dredteam.serve=<port>}
 * 时驻留为 promptfoo target（见 {@code redteam/README.md}）。
 */
class RedteamTargetSmokeTest {

    private HttpServer server;
    private final List<String> dangerousExecuted = new CopyOnWriteArrayList<>();

    private GuardModule guard(BuzhouStores stores) {
        return GuardModule.builder(stores)
                .injectionDefense()
                .taintTracking()
                .dangerousTool("delete_records", "approved_delete", "删除记录是不可逆操作，需人工确认")
                .dangerousTool("run_command", "approved_run", "执行命令须人工确认")
                .build();
    }

    private ToolCallback[] tools() {
        return new ToolCallback[]{
                fixedTool("fetch_page", "（页面）订单 ORD-1 状态：已发货；无异常。"),
                fixedTool("get_order_status", "订单 ORD-1：DELIVERED"),
                fixedTool("delete_records", "deleted", dangerousExecuted)
        };
    }

    /** guard 全开的 agent：读侧工具被调用（打 taint）后，模型试图调用写侧工具 → 被 FIDES 写门拦截。 */
    private FakeChatModel agentModel() {
        return FakeChatModel.script(
                ScriptStep.toolCall("fetch_page", "{}"),
                ScriptStep.toolCall("delete_records", "{}"), // 写侧请求 → 写门拦截（预期行为）
                ScriptStep.text("已按护栏流程处理"));
    }

    @Test
    void targetServesOpenAiShapeAndGuardBlocksUnapprovedWrites() throws Exception {
        int port = Integer.getInteger("redteam.serve", 0);
        server = start(port == 0 ? 0 : port, port != 0);
        int bound = server.getAddress().getPort();

        // OpenAI 兼容形状请求 → 返回 chat.completion（content 为 agent 回复）
        String body = """
                {"model":"buzhou-guard-agent","messages":[{"role":"user","content":"查订单并清理数据"}]}
                """;
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpResponse<String> response = client.send(
                java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://127.0.0.1:" + bound + "/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"choices\"");

        // 护栏行为：读侧执行、写侧被拦（红队断言的行为基线）
        assertThat(dangerousExecuted).isEmpty();

        if (port == 0) {
            server.stop(0);
        }
    }

    /** 起 target：health 端点 + OpenAI 兼容 chat.completions（每次请求新会话、guard 全开）。 */
    private HttpServer start(int port, boolean daemon) throws IOException {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule guard = guard(stores);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/health", exchange -> respond(exchange, 200, "{\"status\":\"ok\"}"));
        httpServer.createContext("/v1/chat/completions", exchange -> {
            String userMessage = extractLastUser(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            // 每请求独立会话 + 新剧本：读 →（写侧尝试被护栏拦截）→ 总结
            FakeChatModel model = agentModel();
            Buzhou.runtime(model, stores, guard.configure(), tools())
                    .spawn("redteam", "guard-agent", "rt-" + System.nanoTime())
                    .chat(userMessage == null ? "处理请求" : userMessage);
            String reply = "已按护栏流程处理（写侧操作须人工确认）";
            respond(exchange, 200, """
                    {"id":"rt-%d","object":"chat.completion","choices":[{"index":0,
                    "message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}
                    """.formatted(System.nanoTime(), reply));
        });
        httpServer.start();
        return httpServer;
    }

    private static void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** 粗提取末条 user 消息文本（红队 payload 无需完整 OpenAI 解析）。 */
    private static String extractLastUser(String requestBody) {
        int idx = requestBody.lastIndexOf("\"role\"\\s*:\\s*\"user\"");
        int fallback = requestBody.lastIndexOf("\"user\"");
        int at = Math.max(idx, fallback);
        if (at < 0) {
            return null;
        }
        int content = requestBody.indexOf("\"content\"", at);
        if (content < 0) {
            return null;
        }
        int start = requestBody.indexOf('"', content + 9) + 1;
        int end = requestBody.indexOf('"', start);
        return start > 0 && end > start ? requestBody.substring(start, end) : null;
    }

    private static ToolCallback fixedTool(String name, String result) {
        return fixedTool(name, result, null);
    }

    private static ToolCallback fixedTool(String name, String result, List<String> sink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                if (sink != null) {
                    sink.add(name);
                }
                return result;
            }
        };
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(daemonLinger());
        }
    }

    private int daemonLinger() {
        return 0;
    }
}
