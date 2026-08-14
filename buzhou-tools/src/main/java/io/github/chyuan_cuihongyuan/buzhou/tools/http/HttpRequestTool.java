package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * http_request — HTTP 调用（危险，默认关、绑定级 opt-in；写方法默认挂 HITL 守卫，
 * GET/HEAD 只读方法不强制——守卫名单归装配侧，本工具不做方法区分）。
 *
 * <p>SSRF 防护默认开：拦内网段与云元数据端点（DNS 解析后校验），可配放行。
 * {@code body} 为写侧长内容参数（{@code bodyPath} 互补，Onload Hook 加载）。
 * 响应体超阈值走 Spill 管道，本工具不截断。
 */
@BuzhouTool(name = "http_request")
public class HttpRequestTool implements ToolCallback {

    /** 写方法集合（spec 06 推演 #6）——方法粒度 HITL 守卫（ticket 27）接线时消费；当前守卫按工具名整体生效。 */
    public static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** impl-49：响应体读入上限（8MB；Content-Length 预检 + 流式截断兜底，防 OOM）。 */
    static final long MAX_RESPONSE_BYTES = 8L * 1024 * 1024;
    /** impl-49：timeoutSeconds 上限（模型自报时长须有上界）。 */
    static final long MAX_TIMEOUT_SECONDS = 300;
    /** impl-49：连接级/逐跳头黑名单（模型不可覆盖）。 */
    private static final java.util.Set<String> BLOCKED_HEADERS = java.util.Set.of(
            "host", "content-length", "transfer-encoding", "connection");
    private static final Set<String> ALLOWED_METHODS =
            Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD");

    private final SsrfGuard ssrfGuard;
    private final Duration defaultTimeout;
    private final HttpClient client;

    public HttpRequestTool(SsrfGuard ssrfGuard, Duration defaultTimeout) {
        this.ssrfGuard = ssrfGuard;
        this.defaultTimeout = defaultTimeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(defaultTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)   // 重定向不自动跟随（SSRF 逐跳校验开放问题）
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("http_request")
                .description("发起 HTTP 请求。内网与云元数据地址默认被 SSRF 防护拦截。"
                        + "长请求体推荐走 bodyPath 让框架自动加载。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "method":{"type":"string","description":"GET / POST / PUT / DELETE / PATCH / HEAD"},
                          "url":{"type":"string","description":"目标 URL；SSRF 校验不通过即拒"},
                          "headers":{"type":"object","description":"请求头"},
                          "body":{"type":"string","description":"请求体"},
                          "bodyPath":{"type":"string","description":"长请求体的互补路径参数，非空时框架自动加载全文覆盖 body"},
                          "timeoutSeconds":{"type":"integer","description":"超时秒数，默认 30"}
                        },"required":["method","url"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String method = args.path("method").asText("").toUpperCase(Locale.ROOT);
            String url = args.path("url").asText("");
            if (!ALLOWED_METHODS.contains(method)) {
                return "http_request 失败：不支持的 method：" + method;
            }
            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException e) {
                return "http_request 失败：非法 URL：" + url;
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return "http_request 失败：仅支持 http/https：" + url;
            }
            String reject = ssrfGuard.check(uri.getHost());
            if (reject != null) {
                return "http_request 拒绝：" + reject;
            }
            long timeoutSeconds = args.path("timeoutSeconds").asLong(defaultTimeout.toSeconds());
            // impl-49：timeoutSeconds 上限校验（此前无上界，模型可自报任意时长）
            if (timeoutSeconds <= 0 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                return "http_request 失败：timeoutSeconds 超出允许范围（1~" + MAX_TIMEOUT_SECONDS + "）";
            }
            String body = args.hasNonNull("body") ? args.path("body").asText() : null;

            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds));
            // impl-49：连接级/逐跳头黑名单——覆盖这些头会破坏 HTTP 语义或 smuggle 向量
            args.path("headers").properties().forEach(h -> {
                String name = h.getKey().trim();
                if (BLOCKED_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    return; // 静默丢弃受控头
                }
                request.header(name, h.getValue().asText());
            });
            request.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));

            // impl-49：响应体有界读入——Content-Length 预检 + 流式截断（此前 ofString 整读进堆，大响应即 OOM 向量）
            HttpResponse<java.io.InputStream> raw = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            long declared = raw.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (declared > MAX_RESPONSE_BYTES) {
                return "http_request 失败：响应体 " + declared + " 字节超过读入上限 "
                        + MAX_RESPONSE_BYTES + " 字节";
            }
            byte[] bytes = raw.body().readNBytes((int) MAX_RESPONSE_BYTES + 1);
            boolean truncated = bytes.length > MAX_RESPONSE_BYTES;
            int len = (int) Math.min(bytes.length, MAX_RESPONSE_BYTES);
            String responseBody = new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_8);
            return "HTTP " + raw.statusCode() + "\n" + responseBody
                    + (truncated ? "\n[响应超过读入上限 8MB，已截断]" : "");
        } catch (Exception e) {
            return "http_request 失败：" + e.getMessage();
        }
    }
}
