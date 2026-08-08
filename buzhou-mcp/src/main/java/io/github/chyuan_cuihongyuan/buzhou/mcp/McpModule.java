package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetProvider;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.DefaultMcpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.Durations;
import io.github.chyuan_cuihongyuan.buzhou.mcp.internal.McpObservability;

import java.time.Duration;
import java.util.Map;

/**
 * MCP 热插拔模块入口（spec 04）：装配 ToolSetProvider → McpClientRegistry 的变更推送链路。
 *
 * <p>用法：
 * <pre>{@code
 * McpModule mcp = McpModule.builder().fromYml(yml).recorder(spanRecorder).build();
 * List<ToolCallback> tools = mcp.registry().toolCallbacksFor(appId, agentName);  // 会话每轮现取
 * ...
 * mcp.close();  // 优雅关闭（等待在途归零或兜底到期）
 * }</pre>
 *
 * <p>清单源三选一：{@link Builder#servers(Map)}（properties 静态）、{@link Builder#store}（DB 轮询
 * 推送）、{@link Builder#provider}（自定义/配置中心适配）。默认空 properties 源。
 */
public final class McpModule implements AutoCloseable {

    private final boolean enabled;
    private final McpClientRegistry registry;
    private final ToolSetProvider provider;

    private McpModule(Builder builder) {
        this.enabled = builder.enabled;
        if (!enabled) {
            this.registry = null;
            this.provider = null;
            return;
        }
        ToolSetProvider resolved = builder.provider;
        if (resolved == null && builder.store != null) {
            resolved = new DbToolSetProvider(builder.store, builder.pollInterval);
        }
        if (resolved == null) {
            resolved = PropertiesToolSetProvider.fromServersMap(builder.servers);
        }
        final ToolSetProvider p = resolved;
        this.provider = p;
        DefaultMcpClientRegistry reg = new DefaultMcpClientRegistry(
                builder.factory, builder.gracePeriod, builder.forceCloseTimeout, builder.recorder,
                builder.policyProvider);
        this.registry = reg;
        // 变更推送：配置源回调 → 差量刷新；坏配置（如重名）拒绝生效、注册表保持旧清单，
        // 记 ERROR Event（phase=refresh）——改配失败必须运维可见（spec 04：全部内部动作进可观测层）
        p.addChangeListener(() -> {
            try {
                reg.refresh(p.currentToolSets());
            } catch (RuntimeException e) {
                McpObservability.refreshRejected(builder.recorder, e);
            }
        });
        reg.refresh(p.currentToolSets());   // 初始建连
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 从 yml map（前缀 buzhou.mcp）解析 enabled/grace-period/force-close-timeout/servers。 */
    public static Builder fromYml(Map<String, Object> ymlConfig) {
        return builder().fromYml(ymlConfig);
    }

    public boolean enabled() {
        return enabled;
    }

    /** 注册表；模块禁用时返回 null。 */
    public McpClientRegistry registry() {
        return registry;
    }

    /** 清单源；模块禁用时返回 null。 */
    public ToolSetProvider provider() {
        return provider;
    }

    /** 优雅关闭：全部条目 DRAINING → 等在途归零或兜底到期 → 停调度/轮询线程。 */
    @Override
    public void close() {
        if (registry != null) {
            registry.shutdown();
        }
        if (provider instanceof DbToolSetProvider db) {
            db.close();
        }
    }

    public static final class Builder {

        private boolean enabled = true;
        private Duration gracePeriod = Duration.ofSeconds(30);
        private Duration forceCloseTimeout = Duration.ofMinutes(5);
        private Duration pollInterval = Duration.ofSeconds(5);
        private Map<String, Object> servers = Map.of();
        private ToolSetProvider provider;
        private ToolSetSpecStore store;
        private PolicyConfigProvider policyProvider;
        private McpConnectionFactory factory = new SpringAiMcpConnectionFactory();
        private SpanRecorder recorder;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** DRAINING 宽限期（默认 30s，spec 04）。 */
        public Builder gracePeriod(Duration gracePeriod) {
            this.gracePeriod = gracePeriod;
            return this;
        }

        /** 强杀兜底（默认 5min，spec 04）。 */
        public Builder forceCloseTimeout(Duration forceCloseTimeout) {
            this.forceCloseTimeout = forceCloseTimeout;
            return this;
        }

        /** DB 源轮询间隔（默认 5s）。 */
        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        /** properties 清单源：{@code buzhou.mcp.servers} 的 map 值。 */
        public Builder servers(Map<String, Object> servers) {
            this.servers = servers == null ? Map.of() : servers;
            return this;
        }

        /** 自定义清单源（配置中心适配等）；优先级最高。 */
        public Builder provider(ToolSetProvider provider) {
            this.provider = provider;
            return this;
        }

        /** DB 清单源存储（次之）；配合 {@link #pollInterval} 轮询推送。 */
        public Builder store(ToolSetSpecStore store) {
            this.store = store;
            return this;
        }

        /**
         * 绑定级配置来源（可选）：提供 {@code buzhou.mcp.bindings.<appId>.<agentName>} 的
         * 绑定级清单（serverName 列表，对全局清单的裁剪视图，spec 04 配置项节）。
         */
        public Builder policyProvider(PolicyConfigProvider policyProvider) {
            this.policyProvider = policyProvider;
            return this;
        }

        /** 连接工厂（测试注入伪实现）。 */
        public Builder factory(McpConnectionFactory factory) {
            this.factory = factory;
            return this;
        }

        /** 可观测采集器；空则热更事件静默（模块独立可用）。 */
        public Builder recorder(SpanRecorder recorder) {
            this.recorder = recorder;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder fromYml(Map<String, Object> ymlConfig) {
            if (ymlConfig == null || ymlConfig.isEmpty()) {
                return this;
            }
            if (ymlConfig.get("enabled") instanceof Boolean b) {
                this.enabled = b;
            }
            Object grace = ymlConfig.get("grace-period");
            if (grace != null) {
                this.gracePeriod = Durations.fromMap(Map.of("grace-period", grace), "grace-period");
            }
            Object force = ymlConfig.get("force-close-timeout");
            if (force != null) {
                this.forceCloseTimeout = Durations.fromMap(
                        Map.of("force-close-timeout", force), "force-close-timeout");
            }
            Object poll = ymlConfig.get("poll-interval");
            if (poll != null) {
                this.pollInterval = Durations.fromMap(Map.of("poll-interval", poll), "poll-interval");
            }
            if (ymlConfig.get("servers") instanceof Map<?, ?> s) {
                this.servers = (Map<String, Object>) s;
            }
            return this;
        }

        public McpModule build() {
            return new McpModule(this);
        }
    }
}
