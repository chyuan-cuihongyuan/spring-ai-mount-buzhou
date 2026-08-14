package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard HTTP 层端到端（ticket 17 验收：引入模块打开后台即得回放/快照/统计；
 * Skill 管理页 CRUD + 上下架 + 绑定）。
 */
class DashboardHttpServerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore store;
    private FakeSkillAdminPort skillAdmin;
    private DashboardModule dashboard;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() {
        store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().observabilityStore();
        seed();
        skillAdmin = new FakeSkillAdminPort();
        dashboard = DashboardModule.builder(store)
                .skillAdmin(skillAdmin)
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

    private void seed() {
        Instant t0 = Instant.parse("2026-08-08T10:00:00Z");
        store.saveSpans(List.of(
                new SpanRecord("ss", null, "sess-http", -1, "SESSION", "session",
                        t0, t0.plusMillis(4000), "OK", Map.of("agent.name", "demo")),
                new SpanRecord("t1", "ss", "sess-http", 1, "TURN", "turn-1",
                        t0, t0.plusMillis(3000), "OK", Map.of()),
                new SpanRecord("m1", "t1", "sess-http", 1, "MODEL_CALL", "model-call",
                        t0.plusMillis(100), t0.plusMillis(900), "OK",
                        Map.of("model.name", "gpt-test", "usage.prompt_tokens", 50,
                                "usage.completion_tokens", 20)),
                new SpanRecord("tc1", "t1", "sess-http", 1, "TOOL_CALL", "tool:todo",
                        t0.plusMillis(1000), t0.plusMillis(1500), "OK",
                        Map.of("tool.name", "todo"))));
        store.saveEvents(List.of(new io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord(
                "e1", "m1", "sess-http", "FINAL_REPLY", t0.plusMillis(900),
                Map.of("content", "完成"))));
        store.saveInjectionSnapshot(new InjectionSnapshot("sess-http", 1, List.of("m1"),
                List.of(), Map.of("historyBudget", 2048), "v1", t0));
    }

    private JsonNode get(String path) throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("GET %s -> %s", path, resp.body()).isEqualTo(200);
        return MAPPER.readTree(resp.body());
    }

    private JsonNode send(String method, String path, Object body) throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(base + path))
                        .method(method, HttpRequest.BodyPublishers.ofString(
                                MAPPER.writeValueAsString(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("%s %s -> %s", method, path, resp.body()).isEqualTo(200);
        return MAPPER.readTree(resp.body());
    }

    @Test
    void servesSinglePageApp() throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(base + "/")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.headers().firstValue("Content-Type")).hasValueSatisfying(
                c -> assertThat(c).contains("text/html"));
        assertThat(resp.body()).contains("开发者控制台");
    }

    @Test
    void sessionListReplaySpansSnapshotStats() throws Exception {
        JsonNode sessions = get("/api/sessions");
        assertThat(sessions.get("items")).hasSize(1);
        assertThat(sessions.get("items").get(0).get("sessionId").asText()).isEqualTo("sess-http");
        assertThat(sessions.get("items").get(0).get("sessionAttributes").get("agent.name").asText())
                .isEqualTo("demo");

        JsonNode replay = get("/api/sessions/sess-http/replay");
        assertThat(replay.get("turns")).hasSize(1);
        assertThat(replay.get("turns").get(0).get("hasSnapshot").asBoolean()).isTrue();
        assertThat(replay.get("turns").get(0).get("events").get(0).get("type").asText())
                .isEqualTo("FINAL_REPLY");

        JsonNode tree = get("/api/sessions/sess-http/spans?view=tree");
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).get("span").get("spanId").asText()).isEqualTo("ss");
        assertThat(tree.get(0).get("children").get(0).get("children")).hasSize(2);

        JsonNode events = get("/api/spans/m1/events");
        assertThat(events).hasSize(1);

        JsonNode snapshot = get("/api/sessions/sess-http/turns/1/snapshot");
        assertThat(snapshot.get("budgetBreakdown").get("historyBudget").asInt()).isEqualTo(2048);
        assertThat(snapshot.get("policyVersion").asText()).isEqualTo("v1");

        JsonNode stats = get("/api/sessions/sess-http/stats");
        assertThat(stats.get("totalPromptTokens").asLong()).isEqualTo(50);
        assertThat(stats.get("perModel").get(0).get("model").asText()).isEqualTo("gpt-test");
        assertThat(stats.get("perTool").get(0).get("tool").asText()).isEqualTo("todo");
    }

    @Test
    void missingSnapshotReturns404() throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(base + "/api/sessions/sess-http/turns/9/snapshot"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(404);
        assertThat(MAPPER.readTree(resp.body()).get("error").asText()).contains("无注入快照");
    }

    @Test
    void skillAdminCrudPublishDisableBinding() throws Exception {
        // 新建（DRAFT）→ 上架 → 下架 → 编辑 → 删除
        send("POST", "/api/skills", Map.of("name", "db-skill", "description", "测试",
                "body", "正文", "allowedTools", List.of("read_file")));
        JsonNode list = get("/api/skills");
        assertThat(list).singleElement().satisfies(s -> {
            assertThat(s.get("name").asText()).isEqualTo("db-skill");
            assertThat(s.get("status").asText()).isEqualTo("DRAFT");
        });

        assertThat(send("POST", "/api/skills/db-skill/publish", Map.of())
                .get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(send("POST", "/api/skills/db-skill/disable", Map.of())
                .get("status").asText()).isEqualTo("DISABLED");
        assertThat(send("PUT", "/api/skills/db-skill", Map.of("description", "改后"))
                .get("description").asText()).isEqualTo("改后");

        // 绑定
        send("PUT", "/api/skill-bindings", Map.of("appId", "app1", "agentName", "agent1",
                "skillNames", List.of("db-skill")));
        JsonNode binding = get("/api/skill-bindings?appId=app1&agentName=agent1");
        assertThat(binding.get("skillNames").get(0).asText()).isEqualTo("db-skill");

        assertThat(send("DELETE", "/api/skills/db-skill", null)
                .get("deleted").asBoolean()).isTrue();
        assertThat(get("/api/skills")).isEmpty();
    }

    @Test
    void skillEndpoints501WithoutPort() throws Exception {
        try (DashboardModule bare = DashboardModule.builder(store).port(0).build().start()) {
            HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + bare.actualPort()
                                    + "/buzhou/api/skills"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(501);
        }
    }

    /** 测试用内存 SkillAdminPort（真身适配 SkillAdminApi 归装配侧）。 */
    static final class FakeSkillAdminPort implements SkillAdminPort {
        private final Map<String, Map<String, Object>> skills = new LinkedHashMap<>();
        private final Map<String, List<String>> bindings = new LinkedHashMap<>();

        @Override
        public List<SkillView> listSkills() {
            return skills.values().stream().map(m -> new SkillView((String) m.get("name"),
                    (String) m.get("description"), "DB", (String) m.get("status"), false)).toList();
        }

        @Override
        public SkillView create(String name, String description, String body,
                                List<String> allowedTools, String createdBy) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("description", description);
            m.put("body", body);
            m.put("status", "DRAFT");
            skills.put(name, m);
            return toView(m);
        }

        @Override
        public SkillView update(String name, String description, String body,
                                List<String> allowedTools) {
            Map<String, Object> m = skills.get(name);
            if (description != null) {
                m.put("description", description);
            }
            return toView(m);
        }

        @Override
        public SkillView publish(String name) {
            skills.get(name).put("status", "PUBLISHED");
            return toView(skills.get(name));
        }

        @Override
        public SkillView disable(String name) {
            skills.get(name).put("status", "DISABLED");
            return toView(skills.get(name));
        }

        @Override
        public boolean delete(String name) {
            return skills.remove(name) != null;
        }

        @Override
        public List<String> getBinding(String appId, String agentName) {
            return bindings.getOrDefault(appId + "/" + agentName, List.of());
        }

        @Override
        public void setBinding(String appId, String agentName, List<String> skillNames) {
            bindings.put(appId + "/" + agentName, new ArrayList<>(skillNames));
        }

        private static SkillView toView(Map<String, Object> m) {
            return new SkillView((String) m.get("name"), (String) m.get("description"),
                    "DB", (String) m.get("status"), false);
        }
    }
}
