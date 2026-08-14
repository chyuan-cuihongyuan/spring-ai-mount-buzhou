package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpClientRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 连接健康装配（impl-50 / spec 14 §F，对齐 guard/resilience 健康范式）：
 * 禁用 UNKNOWN；启用 = UP + 连接状态快照（ACTIVE/DRAINING/建连失败计数）。
 * 纯内存注册表无 DOWN 态（建连失败已由差量刷新跳过 + 计数可见）。
 */
@AutoConfiguration
public class BuzhouMcpHealthAutoConfiguration {

    @Bean
    public McpHealth mcpHealth(Environment env, ObjectProvider<McpClientRegistry> registry) {
        boolean enabled = env.getProperty("buzhou.mcp.enabled", Boolean.class, true);
        return new McpHealth(enabled, registry.getIfAvailable());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class McpHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator mcpHealthIndicator(McpHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }

    /** 健康委托。 */
    public static final class McpHealth implements BuzhouHealth {

        private final boolean enabled;
        private final McpClientRegistry registry;

        McpHealth(boolean enabled, McpClientRegistry registry) {
            this.enabled = enabled;
            this.registry = registry;
        }

        @Override
        public String mechanism() {
            return "mcp";
        }

        @Override
        public Status status() {
            return enabled ? Status.UP : Status.UNKNOWN;
        }

        @Override
        public Map<String, Object> details() {
            if (!enabled) {
                return Map.of("enabled", false);
            }
            if (registry == null) {
                return Map.of("enabled", true);
            }
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("activeConnections", registry.activeConnections());
            details.put("drainingConnections", registry.drainingConnections());
            details.put("dangerousToolCount", registry.dangerousToolNames().size());
            return details;
        }
    }
}
