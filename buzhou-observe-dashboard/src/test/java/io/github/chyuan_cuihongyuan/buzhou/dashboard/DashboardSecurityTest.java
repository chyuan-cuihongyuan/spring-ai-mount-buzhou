package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.config.BuzhouDashboardAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.config.DashboardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-48 / spec 14 §D 安全模型测试：Bearer 鉴权、非 loopback 无 token 拒启动、
 * 请求体上限、分页 clamp、默认 loopback 绑定。
 */
class DashboardSecurityTest {

    private final HttpClient http = HttpClient.newHttpClient();

    /** 配 token 后：无/错 Authorization → 401；正确 Bearer → 200。 */
    @Test
    void bearerTokenRequiredWhenConfigured() throws Exception {
        try (DashboardModule dashboard = DashboardModule
                .builder(store())
                .port(0)
                .authToken("s3cret-token")
                .build()
                .start()) {
            String base = "http://127.0.0.1:" + dashboard.actualPort() + "/buzhou";

            HttpResponse<String> noAuth = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/sessions")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(noAuth.statusCode()).isEqualTo(401);

            HttpResponse<String> wrongAuth = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/sessions"))
                            .header("Authorization", "Bearer wrong").build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(wrongAuth.statusCode()).isEqualTo(401);

            HttpResponse<String> ok = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/sessions"))
                            .header("Authorization", "Bearer s3cret-token").build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(ok.statusCode()).isEqualTo(200);

            // 静态页同样受鉴权保护
            HttpResponse<String> page = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(401);
        }
    }

    /** 未配 token 时既有行为不变（loopback 免鉴权直接可用）。 */
    @Test
    void loopbackWithoutTokenStillOpen() throws Exception {
        try (DashboardModule dashboard = DashboardModule.builder(store()).port(0).build().start()) {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(
                            "http://127.0.0.1:" + dashboard.actualPort() + "/buzhou/api/sessions")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(200);
        }
    }

    /** 非 loopback 且未设 token：装配层启动失败（BuzhouConfigurationException 带修法）。 */
    @Test
    void nonLoopbackWithoutTokenFailsStartup() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "buzhou.observe.dashboard.enabled=true",
                        "buzhou.observe.dashboard.bind-address=0.0.0.0")
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        io.github.chyuan_cuihongyuan.buzhou.core.Buzhou::inMemoryStores)
                .withUserConfiguration(BuzhouDashboardAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(String.valueOf(context.getStartupFailure()))
                            .contains("auth-token")
                            .contains("127.0.0.1");
                });
    }

    /** 非 loopback + token：可启动（WARN 提示属日志面，不阻塞）。 */
    @Test
    void nonLoopbackWithTokenStarts() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "buzhou.observe.dashboard.enabled=true",
                        "buzhou.observe.dashboard.bind-address=0.0.0.0",
                        "buzhou.observe.dashboard.auth-token=t")
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        io.github.chyuan_cuihongyuan.buzhou.core.Buzhou::inMemoryStores)
                .withUserConfiguration(BuzhouDashboardAutoConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(DashboardModule.class));
    }

    /** 请求体超 1MB：413（不再无界读入）。 */
    @Test
    void oversizedBodyRejectedWith413() throws Exception {
        try (DashboardModule dashboard = DashboardModule.builder(store())
                .skillAdmin(stubSkillAdmin()).port(0).build().start()) {
            String big = "x".repeat(1024 * 1024 + 100);
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + dashboard.actualPort() + "/buzhou/api/skills"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"body\":\"" + big + "\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(413);
        }
    }

    /** 分页 size 超界被 clamp：size=100000000 不炸、返回 200。 */
    @Test
    void pageSizeClamped() throws Exception {
        try (DashboardModule dashboard = DashboardModule.builder(store()).port(0).build().start()) {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(
                            "http://127.0.0.1:" + dashboard.actualPort()
                                    + "/buzhou/api/sessions?size=100000000")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(200);
        }
    }

    /** pathPrefix 与 /api 冲突：构建即拒（防自噬路由）。 */
    @Test
    void conflictingPathPrefixRejected() {
        assertThatThrownBy(() -> DashboardModule.builder(store()).port(0).pathPrefix("/api").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/api");
    }

    /** port 越界：属性绑定即失败。 */
    @Test
    void invalidPortFailsFast() {
        assertThatThrownBy(() -> new DashboardProperties(true, 99999, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("port");
    }

    private static ObservabilityStore store() {
        return Buzhou.inMemoryStores().observabilityStore();
    }

    /** 最小 SkillAdminPort stub（413 路径只需通过端口检查，实际不会触达业务方法）。 */
    private static SkillAdminPort stubSkillAdmin() {
        return new SkillAdminPort() {
            @Override
            public java.util.List<SkillAdminPort.SkillView> listSkills() {
                return java.util.List.of();
            }

            @Override
            public SkillAdminPort.SkillView create(String name, String description, String body,
                    java.util.List<String> allowedTools, String createdBy) {
                return null;
            }

            @Override
            public SkillAdminPort.SkillView update(String name, String description, String body,
                    java.util.List<String> allowedTools) {
                return null;
            }

            @Override
            public SkillAdminPort.SkillView publish(String name) {
                return null;
            }

            @Override
            public SkillAdminPort.SkillView disable(String name) {
                return null;
            }

            @Override
            public boolean delete(String name) {
                return false;
            }

            @Override
            public java.util.List<String> getBinding(String appId, String agentName) {
                return java.util.List.of();
            }

            @Override
            public void setBinding(String appId, String agentName, java.util.List<String> skillNames) {
            }
        };
    }
}
