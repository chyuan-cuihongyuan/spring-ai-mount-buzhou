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
import java.util.concurrent.atomic.AtomicLong;

/**
 * http_request — HTTP 调用（危险，默认关、绑定级 opt-in；写方法默认挂 HITL 守卫，
 * GET/HEAD 只读方法不强制——守卫名单归装配侧，本工具不做方法区分）。
 *
 * <p>SSRF 防护默认开：拦内网段与云元数据端点（DNS 解析后校验），可配放行。
 * {@code body} 为写侧长内容参数（{@code bodyPath} 互补，Onload Hook 加载）。
 * 响应体超阈值走 Spill 管道，本工具不截断。
 */
@BuzhouTool(name = "http_request", destructive = true)
public class HttpRequestTool implements ToolCallback {

    /** 写方法集合（spec 06 推演 #6）——方法粒度 HITL 守卫（ticket 27）接线时消费；当前守卫按工具名整体生效。 */
    public static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    /** spec 1629：单头超长（外层返回友好文案、计 OVERSIZE 桶而非 FAILURES）。 */
    static final class HeaderTooLargeException extends RuntimeException {
        HeaderTooLargeException(int length) {
            super("单头值长度 " + length + " 超上限 " + MAX_HEADER_VALUE_CHARS);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** impl-49：响应体读入上限（8MB；Content-Length 预检 + 流式截断兜底，防 OOM）。 */
    static final long MAX_RESPONSE_BYTES = 8L * 1024 * 1024;
    /** impl-49：timeoutSeconds 上限（模型自报时长须有上界）。 */
    static final long MAX_TIMEOUT_SECONDS = 300;
    /** spec 1629 / T2409：body 直传长度上限（64K 字符——超长走 bodyPath Onload 通道）。 */
    static final int MAX_BODY_CHARS = 64 * 1024;
    /** spec 1629：URL 长度上限（8K——HTTP/2 SETTINGS_MAX_HEADER_LIST_STYLE 同思想）。 */
    static final int MAX_URL_CHARS = 8 * 1024;
    /** spec 1629：请求头数量上限（64）。 */
    static final int MAX_HEADERS = 64;
    /** spec 1629：单头值长度上限（8K 字符）。 */
    static final int MAX_HEADER_VALUE_CHARS = 8 * 1024;
    /** impl-49：连接级/逐跳头黑名单（模型不可覆盖）。 */
    private static final java.util.Set<String> BLOCKED_HEADERS = java.util.Set.of(
            "host", "content-length", "transfer-encoding", "connection");
    private static final Set<String> ALLOWED_METHODS =
            Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD");

    private final SsrfGuard ssrfGuard;
    private final Duration defaultTimeout;
    private final HttpClient client;
    /** spec 1603 / T2357：per-host 并发闸（null = 关——默认零行为）。 */
    private final PerHostConcurrencyGuard hostGuard;

    // —— spec 1049 / impl 801：请求量水位与结果分布（Envoy upstream 统计按结局分桶思想；
    // 静态面理由同 R46–R48 域内先例）。守恒：attempts = successes + 六拒绝桶之和。
    // 参数桶（method/url/timeout）指向模型行为；环境桶（ssrf/failures）指向环境与守卫。
    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong SUCCESSES = new AtomicLong();
    private static final AtomicLong METHOD_REJECTS = new AtomicLong();
    private static final AtomicLong URL_REJECTS = new AtomicLong();
    private static final AtomicLong SSRF_REJECTS = new AtomicLong();
    private static final AtomicLong TIMEOUT_PARAM_REJECTS = new AtomicLong();
    private static final AtomicLong OVERSIZE_REJECTS = new AtomicLong();
    /** spec 1603 / T2357：per-host 并发上限拒绝（limit_conn 桶）。 */
    private static final AtomicLong HOST_LIMIT_REJECTS = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    /** 请求量水位与结果分布快照（spec 1049；spec 1603 扩第七桶 hostLimitRejects）。 */
    public record HttpToolStats(long attempts, long successes, long methodRejects,
                                long urlRejects, long ssrfRejects, long timeoutParamRejects,
                                long oversizeRejects, long hostLimitRejects, long failures) {

        /** 拒绝总数（七桶之和）。 */
        public long totalRejects() {
            return methodRejects + urlRejects + ssrfRejects
                    + timeoutParamRejects + oversizeRejects + hostLimitRejects + failures;
        }
    }

    /** 只读快照（守恒 attempts = successes + totalRejects()）。 */
    public static HttpToolStats stats() {
        return new HttpToolStats(ATTEMPTS.get(), SUCCESSES.get(), METHOD_REJECTS.get(),
                URL_REJECTS.get(), SSRF_REJECTS.get(), TIMEOUT_PARAM_REJECTS.get(),
                OVERSIZE_REJECTS.get(), HOST_LIMIT_REJECTS.get(), FAILURES.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        SUCCESSES.set(0);
        METHOD_REJECTS.set(0);
        URL_REJECTS.set(0);
        SSRF_REJECTS.set(0);
        TIMEOUT_PARAM_REJECTS.set(0);
        OVERSIZE_REJECTS.set(0);
        HOST_LIMIT_REJECTS.set(0);
        FAILURES.set(0);
    }

    public HttpRequestTool(SsrfGuard ssrfGuard, Duration defaultTimeout) {
        this(ssrfGuard, defaultTimeout, null);
    }

    /** spec 1603 / T2357：+hostGuard（null = 关——既有构造调用零行为变化）。 */
    public HttpRequestTool(SsrfGuard ssrfGuard, Duration defaultTimeout,
            PerHostConcurrencyGuard hostGuard) {
        this.ssrfGuard = ssrfGuard;
        this.defaultTimeout = defaultTimeout;
        this.hostGuard = hostGuard;
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
        ATTEMPTS.incrementAndGet();
        // spec 1603 / T2357：host 闸占用状态（提升到 try 外——finally 释放用）
        String host = null;
        boolean entered = false;
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String method = args.path("method").asText("").toUpperCase(Locale.ROOT);
            String url = args.path("url").asText("");
            if (!ALLOWED_METHODS.contains(method)) {
                METHOD_REJECTS.incrementAndGet();
                return "http_request 失败：不支持的 method：" + method;
            }
            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException e) {
                URL_REJECTS.incrementAndGet();
                return "http_request 失败：非法 URL：" + url;
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                URL_REJECTS.incrementAndGet();
                return "http_request 失败：仅支持 http/https：" + url;
            }
            // spec 1629 / T2409：输入边界四护栏（Envoy HTTP/2 SETTINGS_MAX_* 思想——
            // 模型自报的超长输入不进执行层；超长 body 走 bodyPath Onload 通道）
            if (url.length() > MAX_URL_CHARS) {
                URL_REJECTS.incrementAndGet();
                return "http_request 失败：url 长度 " + url.length() + " 超上限 " + MAX_URL_CHARS;
            }
            String bodyParam = args.hasNonNull("body") ? args.path("body").asText() : null;
            if (bodyParam != null && bodyParam.length() > MAX_BODY_CHARS) {
                OVERSIZE_REJECTS.incrementAndGet();
                return "http_request 失败：body 长度 " + bodyParam.length()
                        + " 超上限 " + MAX_BODY_CHARS + "（长内容请用 bodyPath 走框架加载）";
            }
            int headerCount = 0;
            if (args.path("headers").isObject()) {
                headerCount = args.path("headers").size();
            }
            if (headerCount > MAX_HEADERS) {
                OVERSIZE_REJECTS.incrementAndGet();
                return "http_request 失败：请求头数量 " + headerCount + " 超上限 " + MAX_HEADERS;
            }
            String reject = ssrfGuard.check(uri.getHost());
            if (reject != null) {
                SSRF_REJECTS.incrementAndGet();
                return "http_request 拒绝：" + reject;
            }
            // spec 1603 / T2357：per-host 并发上限（Nginx limit_conn 思想——超限快速失败不排队）
            host = uri.getHost();
            entered = hostGuard == null || hostGuard.tryEnter(host);
            if (!entered) {
                HOST_LIMIT_REJECTS.incrementAndGet();
                return "http_request 拒绝：host " + host + " 并发已达上限 "
                        + hostGuard.maxPerHost() + "（limit_conn 语义——请稍后重试）";
            }
            long timeoutSeconds = args.path("timeoutSeconds").asLong(defaultTimeout.toSeconds());
            // impl-49：timeoutSeconds 上限校验（此前无上界，模型可自报任意时长）
            if (timeoutSeconds <= 0 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                TIMEOUT_PARAM_REJECTS.incrementAndGet();
                return "http_request 失败：timeoutSeconds 超出允许范围（1~" + MAX_TIMEOUT_SECONDS + "）";
            }
            String body = bodyParam;

            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds));
            // impl-49：连接级/逐跳头黑名单——覆盖这些头会破坏 HTTP 语义或 smuggle 向量
            args.path("headers").properties().forEach(h -> {
                String name = h.getKey().trim();
                if (BLOCKED_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    return; // 静默丢弃受控头
                }
                String value = h.getValue().asText();
                if (value.length() > MAX_HEADER_VALUE_CHARS) {
                    OVERSIZE_REJECTS.incrementAndGet();
                    throw new HeaderTooLargeException(value.length());
                }
                request.header(name, value);
            });
            request.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));

            // impl-49：响应体有界读入——Content-Length 预检 + 流式截断（此前 ofString 整读进堆，大响应即 OOM 向量）
            HttpResponse<java.io.InputStream> raw = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            long declared = raw.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (declared > MAX_RESPONSE_BYTES) {
                OVERSIZE_REJECTS.incrementAndGet();
                return "http_request 失败：响应体 " + declared + " 字节超过读入上限 "
                        + MAX_RESPONSE_BYTES + " 字节";
            }
            byte[] bytes = raw.body().readNBytes((int) MAX_RESPONSE_BYTES + 1);
            boolean truncated = bytes.length > MAX_RESPONSE_BYTES;
            int len = (int) Math.min(bytes.length, MAX_RESPONSE_BYTES);
            String responseBody = new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_8);
            SUCCESSES.incrementAndGet();
            return "HTTP " + raw.statusCode() + "\n" + responseBody
                    + (truncated ? "\n[响应超过读入上限 8MB，已截断]" : "");
        } catch (HeaderTooLargeException tooLarge) {
            return "http_request 失败：" + tooLarge.getMessage();
        } catch (Exception e) {
            FAILURES.incrementAndGet();
            return "http_request 失败：" + e.getMessage();
        } finally {
            if (hostGuard != null && entered) {
                hostGuard.exit(host);
            }
        }
    }
}
