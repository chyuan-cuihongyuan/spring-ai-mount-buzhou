package io.github.chyuan_cuihongyuan.buzhou.dashboard.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardQueryService;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.SkillAdminPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Dashboard 内嵌 HTTP 服务器（spec 03 推演 #12 首发形态：JDK 内置 httpserver，
 * 零新增 Web 依赖；复用业务 Boot 容器归 ticket 20 starter 的 MVC 控制器）。
 *
 * <p>路由（{prefix} 默认 {@code /buzhou}）：
 * <pre>
 * GET  {prefix}/api/sessions?cursor=&size=            会话列表（分页，最近活跃序）
 * GET  {prefix}/api/sessions/{sid}/replay             会话回放（轮次 + Event 流）
 * GET  {prefix}/api/sessions/{sid}/spans?view=flat|tree
 * GET  {prefix}/api/spans/{spanId}/events             单 Span 的 Event 流
 * GET  {prefix}/api/sessions/{sid}/turns/{n}/snapshot 注入快照
 * GET  {prefix}/api/sessions/{sid}/stats              token/耗时统计
 * GET/POST       {prefix}/api/skills                  Skill 列表 / 新建
 * PUT/DELETE     {prefix}/api/skills/{name}           编辑 / 删除
 * POST {prefix}/api/skills/{name}/publish|disable     上架 / 下架
 * GET/PUT {prefix}/api/skill-bindings                 绑定查询 / 设置（appId+agentName）
 * GET  {prefix}/  → index.html                        前端单页（打进 jar 的静态资源）
 * </pre>
 * 错误统一 JSON：{@code {"error": msg}}；400 参数错 / 404 不存在 / 501 Skill 端口未装配。
 */
public class DashboardHttpServer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final DashboardQueryService queries;
    private final SkillAdminPort skillAdmin; // nullable
    private final String prefix;
    private final HttpServer server;
    private final byte[] indexHtml;

    public DashboardHttpServer(DashboardQueryService queries, SkillAdminPort skillAdmin,
                               String pathPrefix, int port) throws IOException {
        this.queries = queries;
        this.skillAdmin = skillAdmin;
        this.prefix = normalizePrefix(pathPrefix);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.server.createContext(prefix, this::route);
        this.indexHtml = readIndexHtml();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int actualPort() {
        return server.getAddress().getPort();
    }

    public String pathPrefix() {
        return prefix;
    }

    private static String normalizePrefix(String pathPrefix) {
        String p = pathPrefix == null || pathPrefix.isBlank() ? "/buzhou" : pathPrefix.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    private static byte[] readIndexHtml() throws IOException {
        try (InputStream in = DashboardHttpServer.class
                .getResourceAsStream("/buzhou-dashboard/index.html")) {
            if (in == null) {
                throw new IOException("缺静态资源 /buzhou-dashboard/index.html（打包遗漏？）");
            }
            return in.readAllBytes();
        }
    }

    // ---- 路由 ----

    private void route(HttpExchange exchange) throws IOException {
        try {
            dispatch(exchange);
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            writeJson(exchange, 400, Map.of("error", "请求体不是合法 JSON"));
        } catch (IllegalStateException e) {
            writeJson(exchange, 501, Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            writeJson(exchange, 404, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            writeJson(exchange, 500, Map.of("error", String.valueOf(e.getMessage())));
        } finally {
            exchange.close();
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String rest = path.length() <= prefix.length() ? "/"
                : path.substring(prefix.length());

        // 静态单页
        if ("GET".equals(method) && ("/".equals(rest) || "/index.html".equals(rest))) {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, indexHtml.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(indexHtml);
            }
            return;
        }
        if (!rest.startsWith("/api/")) {
            throw new NotFoundException("未知路径：" + path);
        }
        String[] seg = rest.substring("/api/".length()).split("/");
        Map<String, String> query = queryParams(exchange.getRequestURI().getRawQuery());

        Object body = switch (seg[0]) {
            case "sessions" -> sessionsRoute(method, seg, query);
            case "spans" -> spansRoute(method, seg);
            case "skills" -> skillsRoute(method, seg, exchange);
            case "skill-bindings" -> bindingsRoute(method, query, exchange);
            default -> throw new NotFoundException("未知 API：" + path);
        };
        if (body != null) {
            writeJson(exchange, 200, body);
        }
    }

    private Object sessionsRoute(String method, String[] seg, Map<String, String> query)
            throws IOException {
        requireGet(method);
        if (seg.length == 1) {
            int size = parseInt(query.get("size"), 50);
            return queries.listSessions(query.get("cursor"), size);
        }
        String sid = seg[1];
        if (seg.length == 3 && "replay".equals(seg[2])) {
            return queries.replay(sid);
        }
        if (seg.length == 3 && "spans".equals(seg[2])) {
            return queries.spans(sid, query.getOrDefault("view", "flat"));
        }
        if (seg.length == 3 && "stats".equals(seg[2])) {
            return queries.stats(sid);
        }
        if (seg.length == 5 && "turns".equals(seg[2]) && "snapshot".equals(seg[4])) {
            Optional<?> snapshot = queries.snapshot(sid, parseInt(seg[3], -1));
            return snapshot.orElseThrow(() -> new NotFoundException(
                    "该轮无注入快照：" + sid + " turn=" + seg[3]));
        }
        throw new NotFoundException("未知 sessions 端点");
    }

    private Object spansRoute(String method, String[] seg) {
        requireGet(method);
        if (seg.length == 3 && "events".equals(seg[2])) {
            return queries.eventsOfSpan(seg[1]);
        }
        throw new NotFoundException("未知 spans 端点");
    }

    private Object skillsRoute(String method, String[] seg, HttpExchange exchange)
            throws IOException {
        SkillAdminPort admin = requireSkillAdmin();
        if (seg.length == 1) {
            if ("GET".equals(method)) {
                return admin.listSkills();
            }
            if ("POST".equals(method)) {
                Map<String, Object> body = readBody(exchange);
                return admin.create(str(body, "name"), str(body, "description"),
                        str(body, "body"), strList(body, "allowedTools"),
                        str(body, "createdBy"));
            }
        } else {
            String name = seg[1];
            if (seg.length == 2 && "PUT".equals(method)) {
                Map<String, Object> body = readBody(exchange);
                return admin.update(name, str(body, "description"), str(body, "body"),
                        strList(body, "allowedTools"));
            }
            if (seg.length == 2 && "DELETE".equals(method)) {
                return Map.of("deleted", admin.delete(name));
            }
            if (seg.length == 3 && "POST".equals(method) && "publish".equals(seg[2])) {
                return admin.publish(name);
            }
            if (seg.length == 3 && "POST".equals(method) && "disable".equals(seg[2])) {
                return admin.disable(name);
            }
        }
        throw new NotFoundException("未知 skills 端点");
    }

    private Object bindingsRoute(String method, Map<String, String> query, HttpExchange exchange)
            throws IOException {
        SkillAdminPort admin = requireSkillAdmin();
        if ("GET".equals(method)) {
            String appId = query.get("appId");
            String agentName = query.get("agentName");
            if (appId == null || appId.isBlank() || agentName == null || agentName.isBlank()) {
                throw new IllegalArgumentException("skill-bindings 查询需携带 appId 与 agentName");
            }
            return Map.of("skillNames", admin.getBinding(appId, agentName));
        }
        if ("PUT".equals(method)) {
            Map<String, Object> body = readBody(exchange);
            admin.setBinding(str(body, "appId"), str(body, "agentName"),
                    strList(body, "skillNames"));
            return Map.of("ok", true);
        }
        throw new NotFoundException("未知 skill-bindings 端点");
    }

    // ---- 工具 ----

    private SkillAdminPort requireSkillAdmin() {
        if (skillAdmin == null) {
            throw new IllegalStateException("Skill 管理端口未装配（SkillAdminPort 未注入）");
        }
        return skillAdmin;
    }

    private static void requireGet(String method) {
        if (!"GET".equals(method)) {
            throw new IllegalArgumentException("仅支持 GET：" + method);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length == 0) {
            return Map.of();
        }
        return MAPPER.readValue(raw, Map.class);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof List<?> list) {
            return ((List<Object>) list).stream().map(String::valueOf).toList();
        }
        return null;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }

    private static Map<String, String> queryParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, String> params = new java.util.HashMap<>();
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static void writeJson(HttpExchange exchange, int status, Object body)
            throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class NotFoundException extends RuntimeException {
        NotFoundException(String message) {
            super(message);
        }
    }
}
