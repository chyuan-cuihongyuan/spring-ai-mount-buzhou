package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Dashboard 装配入口（ticket 17 首发形态，spec 03 推演 #12）：程序化构建 +
 * 独立端口内嵌服务器；复用业务 Boot 容器经 MVC 控制器挂载归 ticket 20 starter。
 *
 * <pre>{@code
 * try (DashboardModule dashboard = DashboardModule.builder(stores.observabilityStore())
 *         .skillAdmin(skillAdminAdapter)   // 可选：SkillAdminApi 适配器
 *         .port(8080)                       // 默认 0 = 随机端口
 *         .build()
 *         .start()) {
 *     // http://localhost:8080/buzhou/
 * }
 * }</pre>
 */
public class DashboardModule implements AutoCloseable {

    private final io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer server;
    private volatile boolean started;

    private DashboardModule(Builder builder) {
        DashboardQueryService queries = new DashboardQueryService(builder.store);
        try {
            this.server = new io.github.chyuan_cuihongyuan.buzhou.dashboard.internal
                    .DashboardHttpServer(queries, builder.skillAdmin, builder.pathPrefix,
                    builder.port, builder.bindAddress, builder.authToken);
        } catch (IOException e) {
            throw new UncheckedIOException("dashboard 内嵌服务器初始化失败", e);
        }
    }

    public static Builder builder(ObservabilityStore store) {
        return new Builder(store);
    }

    public synchronized DashboardModule start() {
        if (!started) {
            server.start();
            started = true;
        }
        return this;
    }

    /** 实际监听端口（配置 0 = 随机端口时经此取真实值）。 */
    public int actualPort() {
        return server.actualPort();
    }

    public String pathPrefix() {
        return server.pathPrefix();
    }

    @Override
    public synchronized void close() {
        if (started) {
            server.stop();
            started = false;
        }
    }

    public static final class Builder {
        private final ObservabilityStore store;
        private SkillAdminPort skillAdmin;
        private int port;
        private String pathPrefix = "/buzhou";
        /** impl-48：默认只绑 loopback；非 loopback 绑定应配 authToken（装配层强制）。 */
        private String bindAddress = "127.0.0.1";
        private String authToken;

        private Builder(ObservabilityStore store) {
            if (store == null) {
                throw new IllegalArgumentException("observabilityStore 不能为空");
            }
            this.store = store;
        }

        /** 注入 Skill 管理端口（装配侧适配 SkillAdminApi）；不注入则 Skill 端点 501。 */
        public Builder skillAdmin(SkillAdminPort skillAdmin) {
            this.skillAdmin = skillAdmin;
            return this;
        }

        /** 独立端口；0 = 随机端口（spec 首发形态语义）。 */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /** 静态资源与 API 前缀，默认 {@code /buzhou}。 */
        public Builder pathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        /** impl-48：绑定地址，默认 127.0.0.1（对齐 Actuator「管理面默认仅本机」立场）。 */
        public Builder bindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
            return this;
        }

        /** impl-48：Bearer 鉴权 token（设置后全部端点要求 Authorization 头）。 */
        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public DashboardModule build() {
            return new DashboardModule(this);
        }
    }
}
