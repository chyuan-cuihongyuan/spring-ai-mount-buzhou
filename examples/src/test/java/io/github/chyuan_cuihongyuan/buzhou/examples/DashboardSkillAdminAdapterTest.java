package io.github.chyuan_cuihongyuan.buzhou.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.policy.InMemoryBindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.BindingPolicyStore;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardModule;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.SkillAdminPort;
import io.github.chyuan_cuihongyuan.buzhou.skill.manage.SkillAdminApi;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.DbSkillRecord;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.InMemorySkillStore;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skill 管理页装配侧集成（ticket 17 验收：Skill 管理页可上架/下架/编辑 DB Skill）：
 * dashboard 的 {@link SkillAdminPort} 由装配侧适配器薄包 buzhou-skills 的
 * {@link SkillAdminApi}（白名单：dashboard 不直依 skills，见 spec 03 推演 #13）。
 *
 * <p>本适配器是 ticket 20 starter 正式装配的参照实现。
 */
class DashboardSkillAdminAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** SkillAdminApi → SkillAdminPort 装配侧适配器。 */
    static final class SkillAdminApiAdapter implements SkillAdminPort {
        private final SkillAdminApi api;

        SkillAdminApiAdapter(SkillAdminApi api) {
            this.api = api;
        }

        @Override
        public List<SkillView> listSkills() {
            return api.listAll().stream().map(s -> new SkillView(s.name(), s.description(),
                    s.source().name(), s.status().name(), s.dbOverridesClasspath())).toList();
        }

        @Override
        public SkillView create(String name, String description, String body,
                                List<String> allowedTools, String createdBy) {
            return toView(api.create(name, description, body, allowedTools, createdBy));
        }

        @Override
        public SkillView update(String name, String description, String body,
                                List<String> allowedTools) {
            return toView(api.update(name, description, body, allowedTools));
        }

        @Override
        public SkillView publish(String name) {
            return toView(api.publish(name));
        }

        @Override
        public SkillView disable(String name) {
            return toView(api.disable(name));
        }

        @Override
        public boolean delete(String name) {
            return api.delete(name);
        }

        @Override
        public List<String> getBinding(String appId, String agentName) {
            return api.getBinding(appId, agentName);
        }

        @Override
        public void setBinding(String appId, String agentName, List<String> skillNames) {
            api.setBinding(appId, agentName, skillNames);
        }

        private static SkillView toView(DbSkillRecord r) {
            return new SkillView(r.name(), r.description(), "DB", r.status().name(), false);
        }
    }

    @Test
    void skillLifecycleThroughDashboardHttp() throws Exception {
        InMemorySkillStore skillStore = new InMemorySkillStore();
        BindingPolicyStore bindingStore = new InMemoryBindingPolicyStore();
        SkillAdminApi adminApi = new SkillAdminApi(skillStore, Map.of(), bindingStore);

        try (DashboardModule dashboard = DashboardModule
                .builder(new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory
                        .InMemoryObservabilityStore())
                .skillAdmin(new SkillAdminApiAdapter(adminApi))
                .port(0).build().start()) {
            String base = "http://127.0.0.1:" + dashboard.actualPort() + "/buzhou/api";
            HttpClient client = HttpClient.newHttpClient();

            // 新建 → 上架 → 编辑 → 下架 → 删除，全链路走真实 SkillAdminApi
            JsonNode created = send(client, "POST", base + "/skills", Map.of(
                    "name", "review-guide", "description", "评审指引",
                    "body", "按清单评审", "allowedTools", List.of("read_file")));
            assertThat(created.get("status").asText()).isEqualTo("DRAFT");

            JsonNode published = send(client, "POST", base + "/skills/review-guide/publish",
                    Map.of());
            assertThat(published.get("status").asText()).isEqualTo("PUBLISHED");
            assertThat(skillStore.findByName("review-guide")).isPresent()
                    .get().satisfies(r -> assertThat(r.status().name()).isEqualTo("PUBLISHED"));

            JsonNode updated = send(client, "PUT", base + "/skills/review-guide",
                    Map.of("description", "评审指引 v2"));
            assertThat(updated.get("description").asText()).isEqualTo("评审指引 v2");

            JsonNode disabled = send(client, "POST", base + "/skills/review-guide/disable",
                    Map.of());
            assertThat(disabled.get("status").asText()).isEqualTo("DISABLED");

            // 绑定落 BindingPolicyStore
            send(client, "PUT", base + "/skill-bindings", Map.of("appId", "app1",
                    "agentName", "agent1", "skillNames", List.of("review-guide")));
            assertThat(adminApi.getBinding("app1", "agent1")).containsExactly("review-guide");
        }
    }

    private static JsonNode send(HttpClient client, String method, String url, Object body)
            throws Exception {
        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method(method, HttpRequest.BodyPublishers.ofString(
                                MAPPER.writeValueAsString(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("%s %s -> %s", method, url, resp.body()).isEqualTo(200);
        return MAPPER.readTree(resp.body());
    }
}
