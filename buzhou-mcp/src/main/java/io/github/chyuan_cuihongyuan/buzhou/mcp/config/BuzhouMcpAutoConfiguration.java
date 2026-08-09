package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigMaps;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.PolicyConfigProvider;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpClientRegistry;
import io.github.chyuan_cuihongyuan.buzhou.mcp.McpModule;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * MCP 热插拔自装配（spec 04 / 09 / ticket 22）。
 *
 * <p>装配 {@link McpModule}（生命周期由容器管理，销毁时优雅关闭）并暴露 {@link McpClientRegistry} bean。
 * 另注册一个 {@link SessionAssemblyCustomizer}：会话装配期按 {@code (appId, agentName)} 解析注册表当前可见
 * 工具，经 {@code SessionAssemblyContext.addToolCallbacks} 注入会话——配了 server 的 MCP 即开箱可用
 * （无 server 时注册表为空，customizer 零开销）。
 *
 * <p>{@link SpanRecorder}（热更事件可观测）与 {@link PolicyConfigProvider}（绑定级清单裁剪）经
 * {@link ObjectProvider} 注入，缺失时降级（事件静默 / 全局清单）。配置经 {@code buzhou.mcp.*}。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.mcp", name = "enabled", matchIfMissing = true)
public class BuzhouMcpAutoConfiguration {

    @Bean(destroyMethod = "close")
    public McpModule mcpModule(Environment env,
                               ObjectProvider<SpanRecorder> recorder,
                               ObjectProvider<PolicyConfigProvider> policyProvider) {
        return McpModule.fromYml(ConfigMaps.sub(env, "buzhou.mcp"))
                .recorder(recorder.getIfAvailable())
                .policyProvider(policyProvider.getIfAvailable())
                .build();
    }

    @Bean
    public McpClientRegistry mcpClientRegistry(McpModule module) {
        return module.registry();
    }

    @Bean
    public SessionAssemblyCustomizer mcpToolInjectingCustomizer(McpClientRegistry registry) {
        return ctx -> {
            List<ToolCallback> tools = registry.toolCallbacksFor(ctx.appId(), ctx.agentName());
            ctx.addToolCallbacks(tools);
        };
    }
}
